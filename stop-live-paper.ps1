param([uint32]$TargetId)
$ErrorActionPreference='Stop'
$target=Get-CimInstance Win32_Process -Filter "ProcessId=$TargetId"
if (!$target -or $target.Name -ne 'java.exe' -or $target.CommandLine -notmatch 'Xmx3G -jar paper\.jar ') { throw 'Paper process changed' }
$parent=Get-CimInstance Win32_Process -Filter "ProcessId=$($target.ParentProcessId)"
Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public static class PaperConsoleSignal {
  [DllImport("kernel32.dll", SetLastError=true)]
  public static extern bool FreeConsole();
  [DllImport("kernel32.dll", SetLastError=true)]
  public static extern bool AttachConsole(uint pid);
  [DllImport("kernel32.dll", SetLastError=true)]
  public static extern uint GetConsoleProcessList([Out] uint[] ids, uint count);
  [DllImport("kernel32.dll", SetLastError=true)]
  public static extern bool SetConsoleCtrlHandler(IntPtr handler, bool add);
  [DllImport("kernel32.dll", SetLastError=true)]
  public static extern bool GenerateConsoleCtrlEvent(uint signal, uint group);
}
'@
$null=[PaperConsoleSignal]::FreeConsole()
if (![PaperConsoleSignal]::AttachConsole($TargetId)) { throw 'Could not attach to Paper process group' }
try {
  $ids=[uint[]]::new(16)
  $count=[PaperConsoleSignal]::GetConsoleProcessList($ids, $ids.Length)
  if ($count -eq 0 -or $count -gt $ids.Length) { throw 'Could not identify attached processes' }
  $extra=$ids[0..($count-1)] | Where-Object { $_ -notin @($PID,$TargetId,$parent.ProcessId,$parent.ParentProcessId) }
  if ($extra) { throw "Unexpected processes in the Paper process group: $extra" }
  if (![PaperConsoleSignal]::SetConsoleCtrlHandler([IntPtr]::Zero,$true)) { throw 'Signal handler failed' }
  if (![PaperConsoleSignal]::GenerateConsoleCtrlEvent(0,0)) { throw 'Shutdown signal failed' }
  Start-Sleep -Seconds 2
} finally {
  $null=[PaperConsoleSignal]::SetConsoleCtrlHandler([IntPtr]::Zero,$false)
  $null=[PaperConsoleSignal]::FreeConsole()
}
$process=Get-Process -Id $TargetId -ErrorAction SilentlyContinue
if ($process -and !$process.WaitForExit(30000)) { throw 'Paper is still shutting down' }
