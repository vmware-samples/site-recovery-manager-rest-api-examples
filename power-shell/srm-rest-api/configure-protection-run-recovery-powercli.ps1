# https://blogs.vmware.com/PowerCLI/2023/06/managing-srm-and-vr-with-powercli-13-1.html
# https://vdc-repo.vmware.com/vmwb-repository/dcr-public/9619cb6d-3975-4bff-aa1f-0e785283a1a9/394a4e63-6031-4af3-8659-751bce3339f9/GUID-875C2A87-0AC9-4B28-9361-5B283AFE114E.html
# https://github.com/vmware-samples/site-recovery-manager-rest-api-examples

# 1. Set PowerCLI configuration
#Get-PowerCLIConfiguration
#if you use self-sign certificate in the environment
#Set-PowerCLIConfiguration -InvalidCertificateAction Ignore

# 2. Set env variabes
$srmLocalHostName = "local-srm.vmware.com"
$localVcUsername = "administrator@vsphere.local"
$localVcPassword = "pass"

$remoteVcUsername = "administrator@vsphere.local"
$remoteVcPassword = "pass"

$pgName = "VMware Explorer Test PG"
$rpName = "VMware Explorer Test RP"

function Wait-TaskToFinish($task)
{
    $notDone = $true
    while ($notDone)
    {
        $taskResult = Invoke-SrmGetTaskInfo -TaskId $task.Id

        if ($taskResult.Progress -ne "100")
        {
            Write-Progress -Activity "Task $( $task.Id ) in Progress" `
                -Status "$( $taskResult.Progress )% Complete:"  `
                -PercentComplete $taskResult.Progress

            Start-Sleep -Seconds 5
        }
        else
        {
            $notDone = $false

            Write-Progress -Activity "Task $( $task.Id ) in Progress" `
                -Status "$( $taskResult.Progress )% Complete:"  `
                -PercentComplete $taskResult.Progress `
                -Completed
        }

    }
}

# Make sure we don't have any stale session
Disconnect-SrmSdkServer *

# Connect to SRM server
$srmConnection = Connect-SrmSdkServer -Server $srmLocalHostName `
    -User $localVcUsername `
    -Password $localVcPassword `
    -RemoteUser $remoteVcUsername `
    -RemotePassword $remoteVcPassword

# Print connection details
$srmConnection

Write-Host "Pairing to use: $( $srmConnection.ConnectedPairing.PairingId )"

# Get replicated VMs from SRM API to be added in SRM Protection Group
$replicatedVmsSrm = Invoke-SrmGetReplicatedVms `
    -PairingId $srmConnection.ConnectedPairing.PairingId `
    -VcenterId $srmConnection.ConnectedPairing.LocalVcServer.Id.Guid

if ($replicatedVmsSrm.List.Count -eq 0)
{
    Write-Warning "No replicated VMs by vSphere Replication found."
}
else
{
    $replicatedVm = $replicatedVmsSrm.List[0]
    Write-Host "Replicated VM: $( $replicatedVm.Name )"

    # Create SRM Protection Group
    $vrPgSpec = Initialize-SrmHbrProtectionGroupSpec -Vms $replicatedVm.Id

    Write-Host "Create protection group spec: $( $vrPgSpec | ConvertTo-JSON )"

    $protectionGroupSpec = Initialize-SrmProtectionGroupCreateSpec `
        -Name $pgName `
        -ReplicationType HBR `
        -ProtectedVcGuid $srmConnection.ConnectedPairing.LocalVcServer.Id.Guid `
        -HbrSpec $vrPgSpec

    $task = Invoke-SrmCreateGroup -PairingId $srmConnection.ConnectedPairing.PairingId `
        -ProtectionGroupCreateSpec $protectionGroupSpec `
        -Server $srmConnection

    Wait-TaskToFinish($task)

    # Wait to make sure the PG is fully initialized
    Start-Sleep -Seconds 5

    $protectionGroups = Invoke-SrmGetAllGroups -PairingId $srmConnection.ConnectedPairing.PairingId `
        -FilterProperty 'Name' `
        -Filter $pgName

    if ($protectionGroups.List.Count -eq 0)
    {
        Write-Warning "No protection groups found"
    }
    else
    {
        Write-Host "Found protection group: $( $protectionGroups.List[0].Name )"

        # Create SRM recovery plan
        $recPlanSpec = Initialize-SrmRecoveryPlanCreateSpec `
            -Name $rpName `
            -ProtectedVcGuid $srmConnection.ConnectedPairing.LocalVcServer.Id.Guid `
            -ProtectionGroups $protectionGroups.List[0].Id

        Write-Host "Create recovery plan spec: $( $recPlanSpec | ConvertTo-JSON )"

        $task = Invoke-SrmCreatePlan -PairingId $srmConnection.ConnectedPairing.PairingId `
            -RecoveryPlanCreateSpec $recPlanSpec `
            -Server $srmConnection

        Wait-TaskToFinish($task)

        # Get SRM recovery plan
        $recoveryPlan = Invoke-SrmGetAllRecoveryPlans -PairingId $srmConnection.ConnectedPairing.PairingId `
            -FilterProperty 'Name' `
            -Filter $rpName `
            -Server $srmConnection

        if ($recoveryPlan.List.Count -eq 0)
        {
            Write-Warning -Message "No recovery plan with name $rpName found"
        }
        else
        {
            Write-Host "Found recovery plan: $( $recoveryPlan.List[0].Name )"

            # Run Test Recovery
            $testPlanSpec = Initialize-SrmTestPlanSpec -SyncData $false

            try
            {
                Write-Host "Trigger test recovery plan"
                $task = Invoke-SrmRunTestRecovery -PairingId $srmConnection.ConnectedPairing.PairingId `
                    -PlanId $recoveryPlan.List[0].Id `
                    -TestPlanSpec $testPlanSpec `
                    -Server $srmConnection

                Wait-TaskToFinish($task)
                Write-Host "Test recovery finished" -ForegroundColor [System.ConsoleColor]::Blue
            }
            catch
            {
                Write-Warning -Message "Test recovery failed"
            }
        }
    }
}

# Close the connections to SRM server:
Disconnect-SrmSdkServer $srmLocalHostName
Write-Host "Successfully logged out of $srmLocalHostName"