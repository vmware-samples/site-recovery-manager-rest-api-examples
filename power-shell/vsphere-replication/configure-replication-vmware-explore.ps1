<#
Script to configure VM replication through vSphere Replication REST API:
- Authentication to VRMS
- Retrieving existing pairings
- Remote Connect-VRMS
- Retrieving VMs suitable for replication
- Query recovery datastores and filter by datastore name
- Query recovery storage policies
- Prepare VM data to configure replication
- Calling create replication
- Logout
#>

param ([Parameter(Mandatory)]
    [string]$vrmsServer,
    [String[]] $vmNames,
    [string]$apiVersion = "v1",
    [boolean]$trace = $false)

$timestamp = Get-Date -f "MM-dd-yyyy-HH.mm.ss"
$output_log = "Logs\ConfigureReplicationVMwareExplore-Log" + $timestamp + ".log"
Start-Transcript -Path "C:\transcripts\$output_log" -NoClobber

# Settings
$printResponses = $trace

$vrmsApiUri = "https://" + $vrmsServer + "/api/rest/vr/" + $apiVersion
$vrmsSessionApiUri = $vrmsApiUri + "/session"
$vrmsPairingsApiUri = $vrmsApiUri + "/pairings"

# Global variables
$global:sessionID = ""
$global:pairing = $null
$global:headers = $null
$global:targetDatastoreId = $null
$global:targetStoragePolicy = $null
$global:vmsData = $null

# Default Replication settings
$global:rpo = 10
$global:mpitInstances = 0
$global:mpitDays = 0
$global:mpitEnabled = $false
$global:networkCompressionEnabled = $false
$global:lwdEncryptionEnabled = $false
$global:quiesceEnabled = $false
$global:autoReplicateNewDisks = $true
$global:destinationDiskFormat = "SAME_AS_SOURCE"

function Connect-VRMS()
{
    $vcUser = Read-HostWithDefault "Enter VC username" "administrator@vsphere.local"
    $vcPassword = Read-Host "Enter VC password" -AsSecureString
    $vcPassword = (New-Object PSCredential 0, $vcPassword).GetNetworkCredential().Password

    $authPair = "$( $vcUser ):$( $vcPassword )"
    $encodedCreds = [System.Convert]::ToBase64String([System.Text.Encoding]::ASCII.GetBytes($authPair))
    $headers = New-Object "System.Collections.Generic.Dictionary[[String],[String]]"
    $headers.Add("Content-Type", "application/json")
    $headers.Add("Authorization", "Basic $encodedCreds")

    Write-Host "Authenticating into [$vrmsSessionApiUri]"

    try
    {
        $response = Invoke-RestMethod $vrmsSessionApiUri -Method 'POST' -Headers $headers
        $response
    }
    catch
    {
        $_.Exception.Response
        Exit 1
    }

    $global:sessionID = $response.session_id

    Trace-WithColor "Session ID for vSphere Replication REST API is: $global:sessionID"

    Set-SessionHeaders

    # Test if successfully logged in
    $response = Invoke-RestMethod $vrmsSessionApiUri -Method 'GET' -Headers $global:headers

    Trace-AsJson($response)
}

function Disconnect-VRMS()
{
    Trace-WithColor "Logging out of VRMS REST API"

    Invoke-RestMethod $vrmsSessionApiUri -Method 'DELETE' -Headers $global:headers

    $global:headers = $null
}

function Select-Pairing
{
    Write-Host "Get all pairings at [$vrmsPairingsApiUri]"

    try
    {
        $response = Invoke-RestMethod $vrmsPairingsApiUri -Method 'GET' -Headers $global:headers
    }
    catch
    {
        $_.Exception.Response
        Disconnect-VRMS
        Exit 1
    }
    $pairings = $response.list

    Write-Host "Retrieved [$( $pairings.Count )] pairings"

    Trace-WithColor "Select pairing from the list:"

    # List all pairings

    Trace-Pairings $pairings

    $pairingIndex = Read-Host "Choose pairing"
    $global:pairing = $pairings[$pairingIndex]

    if (!$global:pairing)
    {
        Trace-WithColor "Cannot find pairing!" ([System.ConsoleColor]::Red)
        Disconnect-VRMS
        Exit 1
    }

    $global:robo = ($global:pairing.local_vc_server.id -eq $global:pairing.remote_vc_server.id)

    Trace-WithColor ("Pairing ID: " + $global:pairing.pairing_id)
}

function Connect-RemoteSite()
{
    if ($global:robo)
    {
        Trace-WithColor "ROBO pairing does not need to authenticate to remote site"
        return
    }

    Write-Host "Logging into remote site"
    $remoteVcUser = Read-HostWithDefault "Enter remote VC username" "administrator@vsphere.local"
    $remoteVcPassword = Read-Host "Enter remote VC password" -AsSecureString
    $remoteVcPassword = (New-Object PSCredential 0, $remoteVcPassword).GetNetworkCredential().Password

    $remoteLoginApiUri = $vrmsApiUri + "/pairings/" + $global:pairing.pairing_id + "/remote-session"
    $authPair = "$( $remoteVcUser ):$( $remoteVcPassword )"
    $encodedCreds = [System.Convert]::ToBase64String([System.Text.Encoding]::ASCII.GetBytes($authPair))
    $headers = New-Object "System.Collections.Generic.Dictionary[[String],[String]]"
    $headers.Add("x-dr-session", "$global:sessionID")
    $headers.Add("Authorization", "Basic $encodedCreds")

    try
    {
        Invoke-RestMethod $remoteLoginApiUri -Method 'POST' -Headers $headers
    }
    catch
    {
        $_.Exception.Response
        Disconnect-VRMS
        Exit 1
    }

    Trace-WithColor "Remote Connect-VRMS is successful!"
}

function Select-TargetDatastore()
{
    $datastoresApiURI = $vrmsApiUri + "/pairings/" + $global:pairing.pairing_id + "/vcenters/" + $global:pairing.remote_vc_server.id + "/datastores"
    Write-Host "Query [$datastoresApiURI]"

    try
    {
        $response = Invoke-RestMethod $datastoresApiURI -Method 'GET' -Headers $global:headers
    }
    catch
    {
        $_.Exception.Response
        Disconnect-VRMS
        Exit 1
    }

    Write-Host "Retrieved [$( $response.list.Count )] datastores"

    Trace-AsJson($response)

    Trace-WithColor "Select target datastore from the list:"

    $datastores = $response.list

    Trace-Datastores $datastores

    $targetDatastoreIndex = Read-Host "Choose target datastore"
    $global:targetDatastoreId = $datastores[$targetDatastoreIndex].id

    Trace-WithColor "Target Datastore ID: $global:targetDatastoreId"
}

function Select-StoragePolicy()
{
    $storagePolicyApiURI = $vrmsApiUri + "/pairings/" + $global:pairing.pairing_id + "/vcenters/" + $global:pairing.remote_vc_server.id + "/storage-policies"

    Write-Host "Query [$storagePolicyApiURI]"

    try
    {
        $response = Invoke-RestMethod $storagePolicyApiURI -Method 'GET' -Headers $global:headers
    }
    catch
    {
        $_.Exception.Response
        Disconnect-VRMS
        Exit 1
    }

    Trace-AsJson($response)

    Write-Host "Retrieved [$( $response.list.Count )] storage policies"

    Trace-WithColor "Select target storage policy from the list:"
    $storagePolicies = $response.list

    Trace-StoragePolicies $storagePolicies

    $targetStoragePolicyIndex = Read-HostWithDefault "Choose target storage policy (leave empty for using datastore default storage policy)"
    if ($targetStoragePolicyIndex -ne $null -And $targetStoragePolicyIndex -ne "")
    {
        $global:targetStoragePolicy = $storagePolicies[$targetStoragePolicyIndex]
    }

    $targetStoragePolicyName = "Datastore Default"
    if ($global:targetStoragePolicy -ne $null)
    {
        $targetStoragePolicyName = $global:targetStoragePolicy.storage_policy_name
    }
    Trace-WithColor "Target storage policy: $targetStoragePolicyName"
}

function Select-VirtualMachines()
{
    if ($vmNames -eq $null -Or $vmNames.Length -eq 0)
    {
        $vmNames = Read-Host "VMs to replicate (comma separated)"
        $vmNames = $vmNames -split ","
    }

    Trace-WithColor "Constructing VM specs for [$vmNames]"

    $vmFilter = ""
    foreach ($vm in $vmNames)
    {
        $vmFilter = $vmFilter + "filter=$vm"
        $vmFilter = $vmFilter + "&"
    }
    $vmFilter = $vmFilter.Substring(0, $vmFilter.Length - 1)

    $vmDetailsApiURI = $vrmsApiUri + "/pairings/" + $global:pairing.pairing_id + "/vcenters/" + $global:pairing.local_vc_server.id + "/vms?suitable_for_replication=true&filter_property=name&$vmFilter"
    Write-Host "Query [$vmDetailsApiURI]"

    try
    {
        $response = Invoke-RestMethod $vmDetailsApiURI -Method 'GET' -Headers $global:headers
    }
    catch
    {
        $_.Exception.Response
        Disconnect-VRMS
        Exit 1
    }

    Write-Host "Retrieved [$( $response.list.Count )] virtual machines"

    Trace-WithColor "********* VM name to ID **********"
    foreach ($vm in $response.list)
    {
        Trace-WithColor ($vm.name + " ------> " + $vm.id)
    }

    Trace-WithColor "Note: VMs which are not suitable for replication are excluded!" ([System.ConsoleColor]::Blue)
    Trace-WithColor "**********************"

    $global:vmsData = $response.list

    if ($global:vmsData.Length -eq 0)
    {
        Trace-WithColor "No VMs can be configured for replication! Aborting.." ([System.ConsoleColor]::Red)
        Disconnect-VRMS
        Exit 1
    }
}
function Invoke-ConfigureReplication()
{

    $specs = @()

    foreach ($vm in $global:vmsData)
    {
        $spec = [PSCustomObject]@{
            auto_replicate_new_disks = $global:autoReplicateNewDisks
            rpo = $global:rpo
            lwd_encryption_enabled = $global:lwdEncryptionEnabled
            mpit_days = $global:mpitDays
            mpit_enabled = $global:mpitEnabled
            mpit_instances = $global:mpitInstances
            network_compression_enabled = $global:networkCompressionEnabled
            quiesce_enabled = $global:quiesceEnabled
            vm_id = $vm.id
            target_replication_server_id = $null
            target_vc_id = $global:pairing.remote_vc_server.id
            disks = @()
        }

        foreach ($dsk in $vm.disks)
        {

            $disk_data = [PSCustomObject]@{
                destination_datastore_id = $global:targetDatastoreId
                destination_storage_policy_id = $global:targetStoragePolicy.storage_policy_id
                destination_disk_format = $global:destinationDiskFormat
                enabled_for_replication = "true"
                use_seeds = "false"

                # We already have the disk information from the previous call
                vm_disk = $dsk
            }

            $spec.disks += $disk_data
        }

        $specs += $spec
    }

    Write-Host "Running configure VM replication for [$( $specs.Length )]"
    Trace-AsJson($specs)

    $jsonBody = ConvertTo-Json -InputObject $specs -Depth 100
    $configureReplicationApiUri = $vrmsApiUri + "/pairings/" + $global:pairing.pairing_id + "/replications"

    Wait-UntilKeypressed "Press any key to configure selected VMs for replication..."

    Write-Host "Query [$configureReplicationApiUri]"

    try
    {
        $response = Invoke-RestMethod $configureReplicationApiUri -Method 'POST' -Headers $headers -Body "$jsonBody"
    }
    catch
    {
        Trace-WithColor "Failed to configure replication!" ([System.ConsoleColor]::Red)
        $_.Exception.Response
        Disconnect-VRMS
        Exit 1
    }

    Trace-WithColor "Configure VM replication successfully called!" ([System.ConsoleColor]::Green)
    Trace-AsJson($response)
}

# Helper methods
function Set-SessionHeaders()
{
    $global:headers = New-Object "System.Collections.Generic.Dictionary[[String],[String]]"
    $global:headers.Add("x-dr-session", "$global:sessionID")
    $global:headers.Add("Content-Type", "application/json")
}

function Trace-Pairings($pairings)
{
    $pairings | Format-Table -Property `
        @{ Label = "Index"; Expression = { ($pairings.IndexOf($_)) } }, `
        @{ Label = "Pairing ID"; Expression = { $_.pairing_id } }, `
        @{ Label = "Local vCenter name"; Expression = { $_.local_vc_server.name } }, `
        @{ Label = "Remote vCenter name"; Expression = { $_.remote_vc_server.name } }
}

function Trace-Datastores($datastores)
{
    $datastores | Format-Table -Property `
        @{ Label = "Index"; Expression = { ($datastores.IndexOf($_)) } },`
        @{ Label = "Name"; Expression = { $_.parent_name + '/' + $_.name } },`
        @{ Label = "Overall Status"; Expression = { $_.overall_status } },`
        @{ Label = "Free Space"; Expression = { $_.free_space } }
}

function Trace-StoragePolicies($storagePolicies)
{
    $storagePolicies | Format-Table -Property `
        @{ Label = "Index"; Expression = { ($storagePolicies.IndexOf($_)) } },`
        @{ Label = "Name"; Expression = { $_.storage_policy_name } },`
        @{ Label = "VM Encryption Supported"; Expression = { $_.vm_encryption_supported } }
}

function Select-ConfigureReplicationSettings($params)
{

    if ($params.ContainsKey("autoReplicateNewDisks") -eq $false)
    {
        $global:autoReplicateNewDisks = Read-HostWithDefault "Auto-replicate new disks" $true
    }
    if ($params.ContainsKey("rpo") -eq $false)
    {
        $global:rpo = Read-HostWithDefault "RPO" 10
    }
    if ($params.ContainsKey("lwdEncryptionEnabled") -eq $false)
    {
        $global:lwdEncryptionEnabled = Read-HostWithDefault "Enable LWD encryption" $false
    }
    if ($params.ContainsKey("networkCompressionEnabled") -eq $false)
    {
        $global:networkCompressionEnabled = Read-HostWithDefault "Enable network compression" $false
    }

    if ($params.ContainsKey("quiesceEnabled") -eq $false)
    {
        $global:quiesceEnabled = Read-HostWithDefault "Enable quiescing" $false
    }

    if ($params.ContainsKey("mpitEnabled") -eq $false)
    {
        $global:mpitEnabled = Read-HostWithDefault "MPIT enabled" $false
    }

    if ($global:mpitEnabled)
    {
        if ($params.ContainsKey("mpitDays") -eq $false)
        {
            $global:mpitDays = Read-HostWithDefault "MPIT days" 0
        }
        if ($params.ContainsKey("mpitInstances") -eq $false)
        {
            $global:mpitInstances = Read-HostWithDefault "MPIT instances" 0
        }
    }
    else
    {
        $global:mpitDays = 0
        $global:mpitInstances = 0
    }

    if ($params.ContainsKey("destinationDiskFormat") -eq $false)
    {
        $global:destinationDiskFormat = Read-HostWithDefault "Destination disk format [SAME_AS_SOURCE, SAME_AS_PRIMARY, AS_DEFINED_IN_PROFILE, FLAT, THICK, NATIVE_THICK, THIN, RDM]" ("SAME_AS_SOURCE")
    }

    Trace-WithColor "Replication settings are:" ([System.ConsoleColor]::Cyan)
    Trace-WithColor "Auto-replicate new disks: $global:autoReplicateNewDisks" ([System.ConsoleColor]::Cyan)
    Trace-WithColor "RPO: $global:rpo" ([System.ConsoleColor]::Cyan)
    Trace-WithColor "Enable LWD encryption: $global:lwdEncryptionEnabled" ([System.ConsoleColor]::Cyan)
    Trace-WithColor "Enable network compression: $global:networkCompressionEnabled" ([System.ConsoleColor]::Cyan)
    Trace-WithColor "Enable quiscing: $global:quiesceEnabled" ([System.ConsoleColor]::Cyan)
    Trace-WithColor "MPIT enabled: $global:mpitEnabled" ([System.ConsoleColor]::Cyan)
    Trace-WithColor "MPIT instances: $global:mpitInstances" ([System.ConsoleColor]::Cyan)
    Trace-WithColor "MPI days: $global:mpitDays" ([System.ConsoleColor]::Cyan)
    Trace-WithColor "Destination disk format: $global:destinationDiskFormat" ([System.ConsoleColor]::Cyan)
}

function Trace-AsJson($element)
{
    if ($printResponses)
    {
        $jsonItem = ConvertTo-Json -InputObject $element -Depth 100
        "$jsonItem"
    }
}

function Read-HostWithDefault()
{
    param (
        [Parameter(Mandatory = $true)]
        [string] $prompt,
        [Parameter(Mandatory = $false)]
        $default = $null
    )

    if ($default -ne $null)
    {
        $prompt = "$prompt [Default: $default]"
    }
    $val = Read-Host $prompt

    if ($val -eq $null -Or $val -eq "")
    {
        $val = $default
    }
    return $val
}

function Trace-WithColor()
{
    param (
        [Parameter(Mandatory = $true)]
        [string] $msg,
        [System.ConsoleColor] $color = [System.ConsoleColor]::Yellow
    )

    Write-Host -ForegroundColor $color "$msg"
}

function Wait-UntilKeypressed($msg = "Press any key to continue...")
{
    $notpressed = $true
    Trace-WithColor $msg ([System.ConsoleColor]::Green)

    while ($notpressed)
    {
        if ([console]::KeyAvailable)
        {
            $notpressed = $false
        }
        Start-Sleep -Milliseconds 500
    }
}

function Disable-SecurityChecks()
{
    Write-Host "Disabling Certificate Checks"

    [System.Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    add-type @"
		using System.Net;
		using System.Security.Cryptography.X509Certificates;
		public class TrustAllCertsPolicy : ICertificatePolicy {
			public bool CheckValidationResult(
			ServicePoint srvPoint, X509Certificate certificate,
			WebRequest request, int certificateProblem) {
				return true;
			}
		}
"@
    [System.Net.ServicePointManager]::CertificatePolicy = New-Object TrustAllCertsPolicy
}


Disable-SecurityChecks
Connect-VRMS
Select-Pairing
Connect-RemoteSite
Select-TargetDatastore
Select-StoragePolicy
Select-VirtualMachines
Trace-AsJson($global:vmsData)
Select-ConfigureReplicationSettings($PSBoundParameters)
Invoke-ConfigureReplication
Disconnect-VRMS

Stop-Transcript 