Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'

$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$iconPath = Join-Path $root 'assets\icons\ai-resume-roaster-icon-512.png'
$outDir = Join-Path $root 'assets\splash'
$outPath = Join-Path $outDir 'ai-resume-roaster-splash-1080x2400.png'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$w = 1080
$h = 2400
$bmp = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

function ColorA($a, $hex) {
    $hex = $hex.TrimStart('#')
    return [System.Drawing.Color]::FromArgb(
        $a,
        [Convert]::ToInt32($hex.Substring(0, 2), 16),
        [Convert]::ToInt32($hex.Substring(2, 2), 16),
        [Convert]::ToInt32($hex.Substring(4, 2), 16)
    )
}

function Brush($hex, $a = 255) {
    return New-Object System.Drawing.SolidBrush (ColorA $a $hex)
}

function PenC($hex, $width = 1, $a = 255) {
    return New-Object System.Drawing.Pen ((ColorA $a $hex), $width)
}

function RoundedPath($x, $y, $width, $height, $radius) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $radius * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $width - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $width - $d, $y + $height - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $height - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

function FillRounded($x, $y, $width, $height, $radius, $brush) {
    $path = RoundedPath $x $y $width $height $radius
    $g.FillPath($brush, $path)
    $path.Dispose()
}

function StrokeRounded($x, $y, $width, $height, $radius, $pen) {
    $path = RoundedPath $x $y $width $height $radius
    $g.DrawPath($pen, $path)
    $path.Dispose()
}

function DrawCenteredText($text, $font, $brush, $x, $y, $width, $height) {
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = [System.Drawing.StringAlignment]::Center
    $fmt.LineAlignment = [System.Drawing.StringAlignment]::Center
    $fmt.Trimming = [System.Drawing.StringTrimming]::EllipsisWord
    $rect = New-Object System.Drawing.RectangleF $x, $y, $width, $height
    $g.DrawString($text, $font, $brush, $rect, $fmt)
    $fmt.Dispose()
}

function DrawGlowCircle($cx, $cy, $r, $hex, $strength = 1.0) {
    for ($i = 10; $i -ge 1; $i--) {
        $alpha = [int]((4 + (11 - $i) * 3.2) * $strength)
        $brush = Brush $hex $alpha
        $rr = $r + ($i * 24)
        $g.FillEllipse($brush, $cx - $rr, $cy - $rr, $rr * 2, $rr * 2)
        $brush.Dispose()
    }
}

# Elegant Material-style blue to purple gradient.
$bgRect = New-Object System.Drawing.Rectangle 0, 0, $w, $h
$bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush $bgRect, (ColorA 255 '#071A3A'), (ColorA 255 '#4C1D95'), 58
$blend = New-Object System.Drawing.Drawing2D.ColorBlend
$blend.Positions = @(0.0, 0.45, 1.0)
$blend.Colors = @((ColorA 255 '#071A3A'), (ColorA 255 '#173A8A'), (ColorA 255 '#4C1D95'))
$bg.InterpolationColors = $blend
$g.FillRectangle($bg, $bgRect)
$bg.Dispose()

# Soft premium lighting.
DrawGlowCircle 540 875 210 '#06B6D4' 1.25
DrawGlowCircle 820 450 260 '#7C3AED' 0.75
DrawGlowCircle 200 1810 260 '#2563EB' 0.65

# Subtle futuristic particles, kept sparse for splash-screen calm.
$linePen = PenC '#93C5FD' 1.4 34
$nodeBrush = Brush '#06B6D4' 72
$nodes = @(
    @(125,320), @(268,260), @(455,342), @(690,250), @(890,360),
    @(160,2045), @(350,1980), @(560,2076), @(760,1978), @(930,2088)
)
for ($i = 0; $i -lt 4; $i++) {
    $a = $nodes[$i]
    $b = $nodes[$i + 1]
    $g.DrawLine($linePen, $a[0], $a[1], $b[0], $b[1])
}
for ($i = 5; $i -lt $nodes.Count - 1; $i++) {
    $a = $nodes[$i]
    $b = $nodes[$i + 1]
    $g.DrawLine($linePen, $a[0], $a[1], $b[0], $b[1])
}
foreach ($n in $nodes) {
    $g.FillEllipse($nodeBrush, $n[0] - 5, $n[1] - 5, 10, 10)
}
$linePen.Dispose()
$nodeBrush.Dispose()

# Central glass halo and pulse rings.
$cx = 540
$cy = 930
$pulsePen1 = PenC '#06B6D4' 5 115
$pulsePen2 = PenC '#FFFFFF' 2 50
$pulsePen3 = PenC '#7C3AED' 4 82
$g.DrawEllipse($pulsePen1, $cx - 282, $cy - 282, 564, 564)
$g.DrawEllipse($pulsePen2, $cx - 335, $cy - 335, 670, 670)
$g.DrawEllipse($pulsePen3, $cx - 232, $cy - 232, 464, 464)
$pulsePen1.Dispose()
$pulsePen2.Dispose()
$pulsePen3.Dispose()

$glass = Brush '#FFFFFF' 18
$glassStroke = PenC '#FFFFFF' 2 54
FillRounded 302 692 476 476 112 $glass
StrokeRounded 302 692 476 476 112 $glassStroke
$glass.Dispose()
$glassStroke.Dispose()

# Logo: reuse the generated app icon for brand consistency.
if (-not (Test-Path $iconPath)) {
    throw "Missing icon asset: $iconPath"
}
$icon = [System.Drawing.Bitmap]::FromFile($iconPath)
$iconSize = 356
$iconX = [int](($w - $iconSize) / 2)
$iconY = 752
$shadow = Brush '#000000' 70
FillRounded ($iconX + 0) ($iconY + 28) $iconSize $iconSize 94 $shadow
$shadow.Dispose()
$g.DrawImage($icon, $iconX, $iconY, $iconSize, $iconSize)
$icon.Dispose()

# Text.
$white = Brush '#FFFFFF' 255
$muted = Brush '#D8E6FF' 222
$soft = Brush '#BFD7FF' 184
$titleFont = New-Object System.Drawing.Font 'Segoe UI', 70, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
$tagFont = New-Object System.Drawing.Font 'Segoe UI', 34, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$bottomFont = New-Object System.Drawing.Font 'Segoe UI', 28, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$microFont = New-Object System.Drawing.Font 'Segoe UI', 22, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)

$titleGlow = Brush '#06B6D4' 38
DrawCenteredText 'AI Resume Roaster' $titleFont $titleGlow 0 1256 $w 96
$titleGlow.Dispose()
DrawCenteredText 'AI Resume Roaster' $titleFont $white 0 1248 $w 96
DrawCenteredText 'AI Powered Resume Analyzer' $tagFont $muted 0 1350 $w 54

# Minimal loading pulse dots to imply animation without clutter.
$dotY = 1466
$dotXs = @(494, 540, 586)
for ($i = 0; $i -lt $dotXs.Count; $i++) {
    $alpha = 120 + ($i * 48)
    $dotBrush = Brush '#06B6D4' $alpha
    $g.FillEllipse($dotBrush, $dotXs[$i] - 8, $dotY - 8, 16, 16)
    $dotBrush.Dispose()
}

# Bottom attribution.
DrawCenteredText 'Powered by Gemini AI' $bottomFont $soft 0 2186 $w 48
$geminiPen = PenC '#06B6D4' 2.4 165
$sparkBrush = Brush '#FFFFFF' 170
$sx = 356
$sy = 2210
$g.DrawLine($geminiPen, $sx - 15, $sy, $sx + 15, $sy)
$g.DrawLine($geminiPen, $sx, $sy - 15, $sx, $sy + 15)
$g.FillEllipse($sparkBrush, $sx - 4, $sy - 4, 8, 8)
$geminiPen.Dispose()
$sparkBrush.Dispose()

# Android safe-area polish: very subtle top and bottom vignettes.
$top = New-Object System.Drawing.Drawing2D.LinearGradientBrush (New-Object System.Drawing.Rectangle 0,0,$w,360), (ColorA 70 '#000000'), (ColorA 0 '#000000'), 90
$g.FillRectangle($top, 0, 0, $w, 360)
$top.Dispose()
$bottom = New-Object System.Drawing.Drawing2D.LinearGradientBrush (New-Object System.Drawing.Rectangle 0,1980,$w,420), (ColorA 0 '#000000'), (ColorA 84 '#000000'), 90
$g.FillRectangle($bottom, 0, 1980, $w, 420)
$bottom.Dispose()
DrawCenteredText 'Powered by Gemini AI' $bottomFont $soft 0 2186 $w 48

$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()

Get-Item $outPath | Select-Object FullName, Length
