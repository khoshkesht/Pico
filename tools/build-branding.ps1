$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$source = [Drawing.Bitmap]::FromFile((Join-Path $projectRoot 'doc/branding/picko-icon-cutout.png'))
$background = [Drawing.Color]::FromArgb(255, 232, 230, 250)

function Write-Icon([int]$Size, [double]$ArtFraction, [string]$OutputPath, [bool]$Transparent = $false, [string]$Mask = '') {
    $bitmap = [Drawing.Bitmap]::new($Size, $Size, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([Drawing.Color]::Transparent)
        $graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $maskPath = [Drawing.Drawing2D.GraphicsPath]::new()
        try {
            if ($Mask -eq 'circle') {
                $maskPath.AddEllipse(0, 0, $Size, $Size)
                $graphics.SetClip($maskPath)
            } elseif ($Mask -eq 'rounded') {
                $diameter = [single]($Size * .40)
                $maskPath.AddArc(0, 0, $diameter, $diameter, 180, 90)
                $maskPath.AddArc(($Size-$diameter), 0, $diameter, $diameter, 270, 90)
                $maskPath.AddArc(($Size-$diameter), ($Size-$diameter), $diameter, $diameter, 0, 90)
                $maskPath.AddArc(0, ($Size-$diameter), $diameter, $diameter, 90, 90)
                $maskPath.CloseFigure()
                $graphics.SetClip($maskPath)
            }
            if (-not $Transparent) {
                $brush = [Drawing.SolidBrush]::new($background)
                try { $graphics.FillRectangle($brush, 0, 0, $Size, $Size) } finally { $brush.Dispose() }
            }
            $artWidth = [single]($Size * $ArtFraction)
            $artHeight = [single]($artWidth * $source.Height / $source.Width)
            $destination = [Drawing.RectangleF]::new(($Size-$artWidth)/2, ($Size-$artHeight)/2, $artWidth, $artHeight)
            $graphics.DrawImage($source, $destination)
        } finally { $maskPath.Dispose() }
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($OutputPath)) | Out-Null
        $bitmap.Save($OutputPath, [Drawing.Imaging.ImageFormat]::Png)
    } finally { $graphics.Dispose(); $bitmap.Dispose() }
}

try {
    $densities = @(@('ldpi',36,81),@('mdpi',48,108),@('hdpi',72,162),@('xhdpi',96,216),@('xxhdpi',144,324),@('xxxhdpi',192,432))
    foreach ($density in $densities) {
        $directory = Join-Path $projectRoot ('app/src/main/res/mipmap-' + $density[0])
        Write-Icon $density[1] .70 (Join-Path $directory 'ic_launcher.png')
        Write-Icon $density[1] .70 (Join-Path $directory 'ic_launcher_round.png') $false 'circle'
        # Entire 50 dp artwork stays inside the adaptive safe circle; verified below.
        Write-Icon $density[2] (50.0/108) (Join-Path $directory 'ic_launcher_foreground.png') $true
    }
    Write-Icon 512 .70 (Join-Path $projectRoot 'doc/branding/picko-512.png')
    Write-Icon 512 .70 (Join-Path $projectRoot 'doc/branding/preview-circle.png') $false 'circle'
    Write-Icon 512 .70 (Join-Path $projectRoot 'doc/branding/preview-rounded.png') $false 'rounded'
    if ([Math]::Sqrt(50*50 + [Math]::Pow(50.0*$source.Height/$source.Width,2)) -gt 66) { throw 'Artwork exceeds adaptive safe circle.' }
    'Generated launcher assets for six densities plus 512px artwork and mask previews.'
} finally { $source.Dispose() }
