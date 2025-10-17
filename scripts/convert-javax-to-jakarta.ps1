# Script to convert javax.* imports to jakarta.* imports
# Handles UTF-8 encoding properly without BOM

$javaFiles = Get-ChildItem -Path "$PSScriptRoot\..\src" -Filter "*.java" -Recurse

foreach ($file in $javaFiles) {
    $content = [System.IO.File]::ReadAllText($file.FullName, [System.Text.UTF8Encoding]::new($false))
    
    # Replace javax imports with jakarta
    $modified = $content -replace 'import javax\.persistence\.', 'import jakarta.persistence.'
    $modified = $modified -replace 'import javax\.validation\.', 'import jakarta.validation.'
    $modified = $modified -replace 'import javax\.servlet\.', 'import jakarta.servlet.'
    $modified = $modified -replace 'import javax\.annotation\.', 'import jakarta.annotation.'
    
    # Only write if content changed
    if ($modified -ne $content) {
        [System.IO.File]::WriteAllText($file.FullName, $modified, [System.Text.UTF8Encoding]::new($false))
        Write-Host "Updated: $($file.FullName)"
    }
}

Write-Host "Conversion complete!"
