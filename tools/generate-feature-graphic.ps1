Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'

$outDir = Join-Path $PSScriptRoot '..\assets\feature-graphic'
$outPath = Join-Path $outDir 'ai-resume-roaster-feature-1024x500.png'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$w = 1024
$h = 500
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

function DrawGlowCircle($cx, $cy, $r, $hex) {
    for ($i = 6; $i -ge 1; $i--) {
        $alpha = [int](8 + (7 - $i) * 7)
        $brush = Brush $hex $alpha
        $rr = $r + ($i * 12)
        $g.FillEllipse($brush, $cx - $rr, $cy - $rr, $rr * 2, $rr * 2)
        $brush.Dispose()
    }
}

function DrawText($text, $font, $brush, $x, $y, $width, $height, $align = 'Near') {
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = [System.Drawing.StringAlignment]::$align
    $fmt.LineAlignment = [System.Drawing.StringAlignment]::Near
    $fmt.Trimming = [System.Drawing.StringTrimming]::EllipsisWord
    $rect = New-Object System.Drawing.RectangleF $x, $y, $width, $height
    $g.DrawString($text, $font, $brush, $rect, $fmt)
    $fmt.Dispose()
}

function DrawCard($x, $y, $width, $height, $radius = 18) {
    $shadow = Brush '#000000' 55
    FillRounded ($x + 0) ($y + 8) $width $height $radius $shadow
    $shadow.Dispose()
    $fill = Brush '#FFFFFF' 22
    FillRounded $x $y $width $height $radius $fill
    $fill.Dispose()
    $stroke = PenC '#FFFFFF' 1 44
    StrokeRounded $x $y $width $height $radius $stroke
    $stroke.Dispose()
}

function DrawCheckIcon($x, $y, $size) {
    $b = Brush '#06B6D4' 255
    $g.FillEllipse($b, $x, $y, $size, $size)
    $b.Dispose()
    $p = PenC '#FFFFFF' 2.8 255
    $p.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $p.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $p.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
    $pts = @(
        (New-Object System.Drawing.PointF ($x + $size * 0.26), ($y + $size * 0.52)),
        (New-Object System.Drawing.PointF ($x + $size * 0.43), ($y + $size * 0.68)),
        (New-Object System.Drawing.PointF ($x + $size * 0.74), ($y + $size * 0.33))
    )
    $g.DrawLines($p, $pts)
    $p.Dispose()
}

function DrawSmallIcon($kind, $x, $y, $size) {
    $p = PenC '#06B6D4' 2.2 235
    $b = Brush '#06B6D4' 85
    if ($kind -eq 'score') {
        $g.DrawArc($p, $x, $y, $size, $size, 200, 250)
        $g.FillEllipse($b, $x + $size * .44, $y + $size * .44, $size * .12, $size * .12)
        $g.DrawLine($p, $x + $size * .5, $y + $size * .5, $x + $size * .76, $y + $size * .28)
    } elseif ($kind -eq 'upload') {
        $g.DrawLine($p, $x + $size * .5, $y + $size * .2, $x + $size * .5, $y + $size * .72)
        $g.DrawLine($p, $x + $size * .3, $y + $size * .42, $x + $size * .5, $y + $size * .2)
        $g.DrawLine($p, $x + $size * .7, $y + $size * .42, $x + $size * .5, $y + $size * .2)
        $g.DrawLine($p, $x + $size * .22, $y + $size * .78, $x + $size * .78, $y + $size * .78)
    } elseif ($kind -eq 'match') {
        $g.DrawEllipse($p, $x + 3, $y + 3, $size - 14, $size - 14)
        $g.DrawLine($p, $x + $size * .68, $y + $size * .68, $x + $size - 3, $y + $size - 3)
    } else {
        $g.DrawRectangle($p, $x + 5, $y + 4, $size - 10, $size - 8)
        $g.DrawLine($p, $x + 9, $y + 12, $x + $size - 9, $y + 12)
        $g.DrawLine($p, $x + 9, $y + 20, $x + $size - 18, $y + 20)
    }
    $p.Dispose()
    $b.Dispose()
}

# Background gradient
$bgRect = New-Object System.Drawing.Rectangle 0, 0, $w, $h
$bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush $bgRect, (ColorA 255 '#071A3A'), (ColorA 255 '#311071'), 25
$blend = New-Object System.Drawing.Drawing2D.ColorBlend
$blend.Positions = @(0.0, 0.48, 1.0)
$blend.Colors = @((ColorA 255 '#071A3A'), (ColorA 255 '#142B71'), (ColorA 255 '#4C1D95'))
$bg.InterpolationColors = $blend
$g.FillRectangle($bg, $bgRect)
$bg.Dispose()

DrawGlowCircle 300 250 130 '#2563EB'
DrawGlowCircle 735 230 135 '#7C3AED'
DrawGlowCircle 515 100 70 '#06B6D4'

# Geometric pattern
$patternPen = PenC '#93C5FD' 1 34
$nodeBrush = Brush '#06B6D4' 70
$nodes = @(
    @(60,70), @(160,38), @(270,100), @(410,54), @(560,92), @(710,44), @(900,86),
    @(96,410), @(220,365), @(365,425), @(540,365), @(700,430), @(865,362), @(975,432)
)
for ($i = 0; $i -lt $nodes.Count - 1; $i++) {
    $a = $nodes[$i]
    $b = $nodes[$i + 1]
    $g.DrawLine($patternPen, $a[0], $a[1], $b[0], $b[1])
}
foreach ($n in $nodes) {
    $g.FillEllipse($nodeBrush, $n[0] - 3, $n[1] - 3, 6, 6)
}
$patternPen.Dispose()
$nodeBrush.Dispose()

# Soft diagonal glass panel behind content
$panelBrush = Brush '#FFFFFF' 15
$panelPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$panelPath.AddPolygon(@(
    (New-Object System.Drawing.Point 418, 36),
    (New-Object System.Drawing.Point 1005, 0),
    (New-Object System.Drawing.Point 980, 500),
    (New-Object System.Drawing.Point 334, 500)
))
$g.FillPath($panelBrush, $panelPath)
$panelBrush.Dispose()
$panelPath.Dispose()

# Smartphone mockup
$phoneX = 78; $phoneY = 44; $phoneW = 260; $phoneH = 412
$phoneShadow = Brush '#000000' 82
FillRounded ($phoneX + 18) ($phoneY + 18) $phoneW $phoneH 36 $phoneShadow
$phoneShadow.Dispose()
$phoneBrush = Brush '#071120' 255
FillRounded $phoneX $phoneY $phoneW $phoneH 36 $phoneBrush
$phoneBrush.Dispose()
$phoneStroke = PenC '#93C5FD' 2 80
StrokeRounded $phoneX $phoneY $phoneW $phoneH 36 $phoneStroke
$phoneStroke.Dispose()
$screenX = $phoneX + 16; $screenY = $phoneY + 18; $screenW = $phoneW - 32; $screenH = $phoneH - 36
$screenPath = RoundedPath $screenX $screenY $screenW $screenH 26
$screenGrad = New-Object System.Drawing.Drawing2D.LinearGradientBrush (New-Object System.Drawing.Rectangle $screenX, $screenY, $screenW, $screenH), (ColorA 255 '#102A66'), (ColorA 255 '#24105B'), 90
$g.FillPath($screenGrad, $screenPath)
$screenGrad.Dispose()
$screenPath.Dispose()
$notch = Brush '#030712' 240
FillRounded ($phoneX + 92) ($phoneY + 26) 76 10 5 $notch
$notch.Dispose()

$white = Brush '#FFFFFF' 255
$muted = Brush '#BFD7FF' 220
$cyan = Brush '#06B6D4' 255
$blue = Brush '#2563EB' 255
$purple = Brush '#7C3AED' 255
$titleFont = New-Object System.Drawing.Font 'Segoe UI', 17, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
$smallFont = New-Object System.Drawing.Font 'Segoe UI', 10, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$miniBold = New-Object System.Drawing.Font 'Segoe UI', 11, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
DrawText 'AI Resume' $titleFont $white ($screenX + 18) ($screenY + 28) 140 28
DrawText 'Dashboard' $smallFont $muted ($screenX + 18) ($screenY + 52) 120 20

# Phone gauge
$gaugePenBg = PenC '#FFFFFF' 7 32
$gaugePen = PenC '#06B6D4' 7 255
$gaugePen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$gaugePen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$g.DrawArc($gaugePenBg, $screenX + 142, $screenY + 26, 58, 58, 145, 250)
$g.DrawArc($gaugePen, $screenX + 142, $screenY + 26, 58, 58, 145, 205)
DrawText '86' (New-Object System.Drawing.Font 'Segoe UI', 20, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)) $white ($screenX + 154) ($screenY + 42) 40 28 'Center'
$gaugePenBg.Dispose(); $gaugePen.Dispose()

DrawCard ($screenX + 18) ($screenY + 104) 190 72 18
DrawSmallIcon 'upload' ($screenX + 32) ($screenY + 122) 34
DrawText 'Resume Upload' $miniBold $white ($screenX + 76) ($screenY + 118) 120 20
DrawText 'PDF scanned and ready' $smallFont $muted ($screenX + 76) ($screenY + 139) 118 18

DrawCard ($screenX + 18) ($screenY + 192) 190 78 18
DrawText 'AI Analysis' $miniBold $white ($screenX + 34) ($screenY + 208) 100 18
$barBg = Brush '#FFFFFF' 36
$bar1 = Brush '#06B6D4' 230
$bar2 = Brush '#7C3AED' 220
FillRounded ($screenX + 34) ($screenY + 236) 150 8 4 $barBg
FillRounded ($screenX + 34) ($screenY + 236) 122 8 4 $bar1
FillRounded ($screenX + 34) ($screenY + 252) 150 8 4 $barBg
FillRounded ($screenX + 34) ($screenY + 252) 96 8 4 $bar2
$barBg.Dispose(); $bar1.Dispose(); $bar2.Dispose()

DrawCard ($screenX + 18) ($screenY + 292) 190 56 18
DrawSmallIcon 'match' ($screenX + 34) ($screenY + 305) 32
DrawText 'Job Match' $miniBold $white ($screenX + 76) ($screenY + 304) 115 18
DrawText 'Skills aligned' $smallFont $muted ($screenX + 76) ($screenY + 325) 115 16

# Floating cards around phone
DrawCard 310 88 190 92 22
DrawSmallIcon 'score' 330 113 40
DrawText 'ATS Score' (New-Object System.Drawing.Font 'Segoe UI', 17, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)) $white 382 108 92 24
DrawText '86% Ready' (New-Object System.Drawing.Font 'Segoe UI', 24, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)) $cyan 382 134 112 34

DrawCard 360 220 210 110 22
DrawText 'AI Resume Review' (New-Object System.Drawing.Font 'Segoe UI', 15, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)) $white 382 238 168 22
for ($i = 0; $i -lt 3; $i++) {
    DrawCheckIcon 384 (272 + ($i * 19)) 13
    $lineBrush = if ($i -eq 0) { Brush '#06B6D4' 185 } elseif ($i -eq 1) { Brush '#7C3AED' 180 } else { Brush '#FFFFFF' 95 }
    FillRounded 407 (274 + ($i * 19)) (94 - ($i * 14)) 7 3 $lineBrush
    $lineBrush.Dispose()
}

# Main typography
$headline = New-Object System.Drawing.Font 'Segoe UI', 42, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
$subtitleFont = New-Object System.Drawing.Font 'Segoe UI', 24, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$featureFont = New-Object System.Drawing.Font 'Segoe UI', 15, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
$glowBrush = Brush '#06B6D4' 42
DrawText 'AI Resume Roaster' $headline $glowBrush 563 104 452 58
$glowBrush.Dispose()
DrawText 'AI Resume Roaster' $headline $white 560 101 452 58
$subtitle = 'Analyze ' + [char]0x2022 + ' Improve ' + [char]0x2022 + ' Get Hired'
DrawText $subtitle $subtitleFont $muted 584 166 360 34

# Feature list
$features = @(
    @('ATS Score', 'score'),
    @('AI Resume Review', 'doc'),
    @('Resume Upload', 'upload'),
    @('Job Match', 'match'),
    @('Cover Letter Generator', 'doc')
)
$startX = 588
$startY = 222
for ($i = 0; $i -lt $features.Count; $i++) {
    $rowX = $startX + (($i % 2) * 187)
    $rowY = $startY + ([Math]::Floor($i / 2) * 62)
    $cardW = 176
    if (($i % 2) -eq 1) { $cardW = 210 }
    if ($i -eq 4) { $cardW = 370 }
    DrawCard $rowX $rowY $cardW 43 16
    DrawCheckIcon ($rowX + 14) ($rowY + 12) 18
    DrawText $features[$i][0] $featureFont $white ($rowX + 42) ($rowY + 11) ($cardW - 52) 24
}

# Career growth micro-visual
$chartPen = PenC '#06B6D4' 4 245
$chartPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$chartPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$chartPts = @(
    (New-Object System.Drawing.PointF 603, 438),
    (New-Object System.Drawing.PointF 660, 414),
    (New-Object System.Drawing.PointF 713, 428),
    (New-Object System.Drawing.PointF 772, 398)
)
$g.DrawLines($chartPen, $chartPts)
$arrowPen = PenC '#FFFFFF' 3 230
$g.DrawLine($arrowPen, 772, 398, 758, 397)
$g.DrawLine($arrowPen, 772, 398, 767, 412)
$chartPen.Dispose(); $arrowPen.Dispose()
DrawText 'Career Growth Insights' (New-Object System.Drawing.Font 'Segoe UI', 16, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)) $muted 602 455 230 24

# Floating AI particles
$particleBrush = Brush '#06B6D4' 160
$particlePen = PenC '#06B6D4' 1 64
$particleCenters = @(@(516,78), @(542,118), @(502,158), @(900,190), @(944,236), @(856,304), @(926,405), @(438,384))
foreach ($p in $particleCenters) {
    $g.FillEllipse($particleBrush, $p[0] - 3, $p[1] - 3, 6, 6)
}
for ($i = 0; $i -lt $particleCenters.Count - 1; $i += 2) {
    $a = $particleCenters[$i]
    $b = $particleCenters[$i + 1]
    $g.DrawLine($particlePen, $a[0], $a[1], $b[0], $b[1])
}
$particleBrush.Dispose(); $particlePen.Dispose()

# Save and dispose
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()

Get-Item $outPath | Select-Object FullName, Length
