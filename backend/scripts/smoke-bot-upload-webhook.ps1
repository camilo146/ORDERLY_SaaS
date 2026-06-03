param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$UseMockEvolution = "true",
    [int]$MockEvolutionPort = 18085,
    [string]$AdminEmail = "admin@orderly.local",
    [string]$AdminPassword = "Orderly123!"
)

$ErrorActionPreference = "Stop"
$useMock = $UseMockEvolution.ToLowerInvariant() -in @("true", "1", "yes", "y")

function Invoke-OrderlyJson {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Url,
        [hashtable]$Headers,
        [object]$Body
    )

    $params = @{
        Method = $Method
        Uri = $Url
        ContentType = "application/json"
    }

    if ($Headers) {
        $params.Headers = $Headers
    }

    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 12 -Compress)
    }

    return Invoke-RestMethod @params
}

function Upload-ProductImage {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][string]$BearerToken,
        [Parameter(Mandatory = $true)][string]$BusinessId,
        [Parameter(Mandatory = $true)][string]$FilePath
    )

    Add-Type -AssemblyName System.Net.Http

    $handler = New-Object System.Net.Http.HttpClientHandler
    $client = New-Object System.Net.Http.HttpClient($handler)
    try {
        $request = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Post, $Url)
        $request.Headers.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $BearerToken)
        $request.Headers.Add("X-Business-Id", $BusinessId)

        $multipart = New-Object System.Net.Http.MultipartFormDataContent
        $fileBytes = [System.IO.File]::ReadAllBytes($FilePath)
        $fileContent = New-Object System.Net.Http.ByteArrayContent(,$fileBytes)
        $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("image/png")
        $multipart.Add($fileContent, "file", [System.IO.Path]::GetFileName($FilePath))

        $request.Content = $multipart
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        $responseBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()

        if (-not $response.IsSuccessStatusCode) {
            throw "Upload fallido. HTTP $($response.StatusCode): $responseBody"
        }

        return ($responseBody | ConvertFrom-Json)
    }
    finally {
        $client.Dispose()
    }
}

function New-TinyPng {
    param([Parameter(Mandatory = $true)][string]$Path)

    $tinyPngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9WnM4DkAAAAASUVORK5CYII="
    [System.IO.File]::WriteAllBytes($Path, [Convert]::FromBase64String($tinyPngBase64))
}

function Start-MockEvolutionServer {
    param(
        [Parameter(Mandatory = $true)][int]$Port,
        [Parameter(Mandatory = $true)][string]$LogFile
    )

    if (Test-Path $LogFile) {
        Remove-Item $LogFile -Force
    }

    $job = Start-Job -ArgumentList $Port, $LogFile -ScriptBlock {
        param($InnerPort, $InnerLogFile)

        $listener = New-Object System.Net.HttpListener
        $listener.Prefixes.Add("http://*:$InnerPort/")
        $listener.Start()

        try {
            while ($true) {
                $context = $listener.GetContext()
                $request = $context.Request
                $reader = New-Object System.IO.StreamReader($request.InputStream, $request.ContentEncoding)
                $body = $reader.ReadToEnd()
                $reader.Dispose()

                $line = [pscustomobject]@{
                    timestamp = (Get-Date).ToString("o")
                    method = $request.HttpMethod
                    path = $request.Url.AbsolutePath
                    body = $body
                } | ConvertTo-Json -Compress

                Add-Content -Path $InnerLogFile -Value $line

                $responseJson = "{}"
                $status = 200

                if ($request.Url.AbsolutePath -like "/instance/connect/*") {
                    $responseJson = '{"code":"CONNECTED","base64":null}'
                }

                $bytes = [System.Text.Encoding]::UTF8.GetBytes($responseJson)
                $context.Response.StatusCode = $status
                $context.Response.ContentType = "application/json"
                $context.Response.OutputStream.Write($bytes, 0, $bytes.Length)
                $context.Response.OutputStream.Close()
            }
        }
        finally {
            $listener.Stop()
            $listener.Close()
        }
    }

    return $job
}

function Get-MockEntries {
    param([string]$LogFile)

    if (-not (Test-Path $LogFile)) {
        return @()
    }

    $entries = @()
    foreach ($line in (Get-Content -Path $LogFile)) {
        if (-not [string]::IsNullOrWhiteSpace($line)) {
            $entries += ($line | ConvertFrom-Json)
        }
    }
    return $entries
}

function Assert-ContainsSequence {
    param(
        [string[]]$Messages,
        [string[]]$ExpectedFragments
    )

    $idx = 0
    foreach ($fragment in $ExpectedFragments) {
        $found = $false
        while ($idx -lt $Messages.Count) {
            if ($Messages[$idx] -like "*$fragment*") {
                $found = $true
                $idx++
                break
            }
            $idx++
        }

        if (-not $found) {
            throw "No se encontró el fragmento esperado en el orden correcto: '$fragment'"
        }
    }
}

$mockJob = $null
$logFile = Join-Path $env:TEMP ("orderly-evolution-mock-" + [guid]::NewGuid().ToString() + ".log")
$tempImageFile = Join-Path $env:TEMP ("orderly-smoke-" + [guid]::NewGuid().ToString() + ".png")

try {
    if ($useMock) {
        $mockJob = Start-MockEvolutionServer -Port $MockEvolutionPort -LogFile $logFile
        Start-Sleep -Milliseconds 600
        Write-Host "Mock Evolution iniciado en puerto $MockEvolutionPort"
        Write-Host "IMPORTANTE: backend debe estar levantado con EVOLUTION_API_URL=http://localhost:$MockEvolutionPort"
    }

    $health = Invoke-RestMethod -Method Get -Uri ($BaseUrl + "/api/v1/health")
    $login = Invoke-OrderlyJson -Method "Post" -Url ($BaseUrl + "/api/v1/auth/login") -Body @{
        email = $AdminEmail
        password = $AdminPassword
    }

    $token = $login.accessToken
    $authHeaders = @{ Authorization = "Bearer $token" }

    $suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $business = Invoke-OrderlyJson -Method "Post" -Url ($BaseUrl + "/api/v1/businesses") -Headers $authHeaders -Body @{
        name = "Smoke Bot $suffix"
        businessType = "restaurant"
    }

    $tenantHeaders = @{
        Authorization = "Bearer $token"
        "X-Business-Id" = $business.id
    }

    $product = Invoke-OrderlyJson -Method "Post" -Url ($BaseUrl + "/api/v1/businesses/$($business.id)/products") -Headers $tenantHeaders -Body @{
        name = "Combo Bot $suffix"
        description = "Producto para smoke test"
        price = 16900
    }

    New-TinyPng -Path $tempImageFile
    $uploadParams = @{
        Url = ($BaseUrl + "/api/v1/businesses/$($business.id)/products/$($product.id)/image")
        BearerToken = $token
        BusinessId = $business.id
        FilePath = $tempImageFile
    }
    $productWithImage = Upload-ProductImage @uploadParams

    if ([string]::IsNullOrWhiteSpace($productWithImage.imageUrl)) {
        throw "El upload no devolvió imageUrl."
    }

    $catalog = Invoke-RestMethod -Method Get -Uri ($BaseUrl + "/api/v1/businesses/$($business.id)/products") -Headers $tenantHeaders
    if (-not $catalog -or $catalog.Count -lt 1) {
        throw "Catálogo vacío luego de crear producto."
    }

    $slug = $business.slug
    $phone = "573001112233"
    $jid = "$phone@s.whatsapp.net"
    $webhookUrl = "$BaseUrl/webhook/whatsapp/$slug"

    $connectionPayload = @{
        event = "connection.update"
        instance = "orderly-$slug"
        data = @{ state = "open" }
    }
    Invoke-OrderlyJson -Method "Post" -Url $webhookUrl -Body $connectionPayload | Out-Null

    $userMessages = @("hola", "1", "1", "2", "Calle 123 #45-67", "1", "si")

    foreach ($text in $userMessages) {
        $payload = @{
            event = "messages.upsert"
            instance = "orderly-$slug"
            data = @{
                messages = @(
                    @{
                        key = @{ remoteJid = $jid; fromMe = $false }
                        message = @{ conversation = $text }
                    }
                )
            }
        }

        Invoke-OrderlyJson -Method "Post" -Url $webhookUrl -Body $payload | Out-Null
        Start-Sleep -Milliseconds 300
    }

    $textMessages = @()
    $mediaCount = 0

    if ($useMock) {
        $entries = Get-MockEntries -LogFile $logFile
        $sendTextEntries = $entries | Where-Object { $_.path -like "/message/sendText/*" }
        $sendMediaEntries = $entries | Where-Object { $_.path -like "/message/sendMedia/*" }
        $mediaCount = ($sendMediaEntries | Measure-Object).Count

        if (($sendTextEntries | Measure-Object).Count -eq 0) {
            throw "No se capturaron mensajes salientes del bot. Verifica que backend use EVOLUTION_API_URL=http://localhost:$MockEvolutionPort"
        }

        foreach ($entry in $sendTextEntries) {
            $parsed = $entry.body | ConvertFrom-Json
            if ($parsed.text) {
                $textMessages += [string]$parsed.text
            }
        }

        Assert-ContainsSequence -Messages $textMessages -ExpectedFragments @(
            "¿Qué deseas hacer?",
            "Nuestros productos",
            "¿Cuántas unidades",
            "¿Cuál es tu dirección",
            "¿Cómo vas a pagar?",
            "¿Confirmas?",
            "¡Pedido confirmado!"
        )

        if ($mediaCount -lt 1) {
            throw "No se detectaron envíos de imagen del catálogo (sendMedia)."
        }
    }

    [pscustomobject]@{
        ok = $true
        health = $health.status
        businessId = $business.id
        businessSlug = $slug
        productId = $product.id
        productImageUrl = $productWithImage.imageUrl
        catalogItems = $catalog.Count
        outboundTextMessagesCaptured = $textMessages.Count
        outboundMediaMessagesCaptured = $mediaCount
        notes = if ($useMock) { "Flujo validado con mock Evolution." } else { "Flujo webhook ejecutado sin validación de mensajes salientes." }
    } | ConvertTo-Json -Depth 6
}
finally {
    if (Test-Path $tempImageFile) {
        Remove-Item $tempImageFile -Force
    }

    if ($mockJob) {
        Stop-Job $mockJob -ErrorAction SilentlyContinue | Out-Null
        Remove-Job $mockJob -Force -ErrorAction SilentlyContinue | Out-Null
    }

    if (Test-Path $logFile) {
        Remove-Item $logFile -Force
    }
}
