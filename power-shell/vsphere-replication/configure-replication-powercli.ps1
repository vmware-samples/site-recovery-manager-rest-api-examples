# https://blogs.vmware.com/PowerCLI/2023/06/managing-srm-and-vr-with-powercli-13-1.html
# https://vdc-repo.vmware.com/vmwb-repository/dcr-public/9619cb6d-3975-4bff-aa1f-0e785283a1a9/394a4e63-6031-4af3-8659-751bce3339f9/GUID-875C2A87-0AC9-4B28-9361-5B283AFE114E.html
# https://github.com/vmware-samples/site-recovery-manager-rest-api-examples

# 1. Set PowerCLI configuration
# Get-PowerCLIConfiguration
# if you use self-sign certificate in the environment
# Set-PowerCLIConfiguration -InvalidCertificateAction Ignore

# 2. Set env variabes
$vrLocalHostName = "local-vrms.vmware.com"
$localVcUsername = "administrator@vsphere.local"
$localVcPassword = "pass"

$vcRemoteHostName = "remote-vc.vmware.com"
$vcRemoteName = "VC Boston"
$remoteVcUsername = "administrator@vsphere.local"
$remoteVcPassword = "pass"

$vmNameToReplicate = "Tiny-VM"
$targetDatastoreName = "vsanDatastore"
$rpo = 60
$networkCompressionEnabled = $false
$mpitEnabled = $false
$autoReplicateNewDisks = $true
$lwdEncryptionEnabled = $false
$mpitInstances = 0
$mpitDays = 0

function Wait-TaskToFinish($task)
{
    $notDone = $true
    while ($notDone)
    {
        $taskResult = Invoke-VrGetTaskInfo -TaskId $task.Id

        if ($taskResult.Progress -ne "100" -and $taskResult.CompleteTime -eq $null)
        {
            Write-Host "Task NOT done"

            Write-Progress -Activity "Task $( $task.Id ) in Progress" `
                -Status "$( $taskResult.Progress )% Complete:"  `
                -PercentComplete $taskResult.Progress

            Start-Sleep -Seconds 5
        }
        else
        {
            Write-Host "Task done"

            $notDone = $false

            Write-Progress -Activity "Task $( $task.Id ) in Progress" `
                -Status "$( $taskResult.Progress )% Complete:"  `
                -PercentComplete $taskResult.Progress `
                -Completed
        }

    }
}

# Make sure we don't have any stale session
Disconnect-VrServer *

# Setup VR connection - login to both sites
$vrConnection = Connect-VrServer -Server $vrLocalHostName `
   -User $localVcUsername `
   -Password $localVcPassword `
   -RemoteServer $vcRemoteName `
   -RemoteUser $remoteVcUsername `
   -RemotePassword $remoteVcPassword `
   -Debug `
   -IgnoreInvalidCertificate

# Print connection details
$vrConnection

# Get Remote VR server paired server
$remoteVrPairing = $vrConnection.ConnectedPairings[$vcRemoteHostName].Pairing

Write-Host "Pairing to use: $( $remoteVrPairing.PairingId )"

# Get VMs suitable for replications
$vmsToReplicate = Invoke-VrGetLocalVms -PairingId $remoteVrPairing.PairingId `
   -SuitableForReplication $True `
   -VcenterId $remoteVrPairing.LocalVcServer.Id.Guid `
   -FilterProperty 'Name' `
   -Filter $vmNameToReplicate

Write-Host "VMs available for replication: $( $vmsToReplicate.List )"

if ($vmsToReplicate.List.Count -eq 0)
{
    Write-Host "No VMs found. Aborting.."
}
else
{
    # Get the 1st VM suitable for replication
    $vm = $vmsToReplicate.List[0]

    Write-Host "VM to replicate: $( $vm.Name )"

    # Retrieve VR-capable target datastores
    $targetDatastores = Invoke-VrGetVrCapableTargetDatastores -PairingId $remoteVrPairing.PairingId `
       -VcenterId $remoteVrPairing.RemoteVcServer.Id.Guid `
       -FilterProperty "Name" `
       -Filter $targetDatastoreName

    if ($targetDatastores.List.Count -eq 0)
    {
        Write-Host "No target datastores found. Aborting.."
    }
    else
    {
        $targetDatastore = $targetDatastores.List[0]
        Write-Host "Target datastore: $( $targetDatastore.Id )"

        # Construct configure replication spec
        $replicationVmDisks = @()
        $vm.Disks | ForEach-Object {
            $replicationVmDisks += Initialize-VrConfigureReplicationVmDisk -VmDisk $_ `
              -EnabledForReplication $true `
              -DestinationDatastoreId $targetDatastore.Id `
              -DestinationDiskFormat 'SAMEASSOURCE'
        }

        $replicationSpec = Initialize-VrConfigureReplicationSpec `
           -Rpo $rpo `
           -NetworkCompressionEnabled $networkCompressionEnabled `
           -MpitEnabled $mpitEnabled `
           -AutoReplicateNewDisks $autoReplicateNewDisks `
           -LwdEncryptionEnabled $lwdEncryptionEnabled `
           -MpitInstances $mpitInstances `
           -MpitDays $mpitDays `
           -Disks $replicationVmDisks `
           -TargetVcId $remoteVrPairing.RemoteVcServer.Id.Guid `
           -VmId $vm.Id

        Write-Host "Replication spec: $( $replicationSpec | ConvertTo-JSON )"

        # Configure replication
        $task = Invoke-VrConfigureReplication -PairingId $remoteVrPairing.PairingId `
            -ConfigureReplicationSpec $replicationSpec

        Write-Host "All tasks: $( $task.List | ConvertTo-Json ) "
        Write-Host "Configure replication task: $( $task.List[0].Id )"

        Wait-TaskToFinish($task.List[0])

        Write-Host "The task to configure VM $( $vm.Name ) for replication is completed."
    }
}

# Close the connection to VR server:
Disconnect-VrServer $vrLocalHostName
Write-Host "Successfully logged out of $vrLocalHostName"