$ErrorActionPreference = 'Stop'

$baseUrl = $env:ORDERLY_BASE_URL
if ([string]::IsNullOrWhiteSpace($baseUrl)) {
    $baseUrl = 'http://localhost:8080'
}

function Invoke-OrderlyJson {
    param(
        [Parameter(Mandatory = $true)] [string] $Method,
        [Parameter(Mandatory = $true)] [string] $Url,
        [hashtable] $Headers,
        [object] $Body
    )

    $params = @{
        Method = $Method
        Uri = $Url
        ContentType = 'application/json'
    }

    if ($Headers) {
        $params.Headers = $Headers
    }

    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 8 -Compress)
    }

    return Invoke-RestMethod @params
}

$health = Invoke-RestMethod -Method Get -Uri ($baseUrl + '/api/v1/health')
$login = Invoke-OrderlyJson -Method 'Post' -Url ($baseUrl + '/api/v1/auth/login') -Body @{
    email = 'admin@orderly.local'
    password = 'Orderly123!'
}

$authHeaders = @{ Authorization = 'Bearer ' + $login.accessToken }
$business = Invoke-OrderlyJson -Method 'Post' -Url ($baseUrl + '/api/v1/businesses') -Headers $authHeaders -Body @{
    name = 'Smoke Test Bistro'
    businessType = 'restaurant'
}

$tenantHeaders = @{ 
    Authorization = 'Bearer ' + $login.accessToken
    'X-Business-Id' = $business.id
}

$product = Invoke-OrderlyJson -Method 'Post' -Url ($baseUrl + '/api/v1/businesses/' + $business.id + '/products') -Headers $tenantHeaders -Body @{
    name = 'Combo Ejecutivo'
    description = 'Almuerzo del dia'
    price = 18000
}

$order = Invoke-OrderlyJson -Method 'Post' -Url ($baseUrl + '/api/v1/businesses/' + $business.id + '/orders') -Headers $tenantHeaders -Body @{
    customerName = 'Laura'
    customerWhatsapp = '+573001112233'
    deliveryType = 'pickup'
    notes = 'Sin hielo'
    items = @(
        @{
            productId = $product.id
            quantity = 2
            notes = 'Extra salsa'
        }
    )
}

$updatedOrder = Invoke-OrderlyJson -Method 'Patch' -Url ($baseUrl + '/api/v1/businesses/' + $business.id + '/orders/' + $order.id + '/status') -Headers $tenantHeaders -Body @{
    status = 'READY'
}

$businessDashboard = Invoke-RestMethod -Method Get -Uri ($baseUrl + '/api/v1/businesses/' + $business.id + '/dashboard') -Headers $tenantHeaders
$superAdminLogin = Invoke-OrderlyJson -Method 'Post' -Url ($baseUrl + '/api/v1/auth/login') -Body @{
    email = 'superadmin@orderly.local'
    password = 'Orderly123!'
}
$superAdminHeaders = @{ Authorization = 'Bearer ' + $superAdminLogin.accessToken }
$systemOverview = Invoke-RestMethod -Method Get -Uri ($baseUrl + '/api/v1/admin/overview') -Headers $superAdminHeaders
$operatorLogin = Invoke-OrderlyJson -Method 'Post' -Url ($baseUrl + '/api/v1/auth/login') -Body @{
    email = 'ops@orderly.local'
    password = 'Orderly123!'
}
$operatorHeaders = @{ Authorization = 'Bearer ' + $operatorLogin.accessToken }
$operatorOverview = Invoke-RestMethod -Method Get -Uri ($baseUrl + '/api/v1/operator/overview') -Headers $operatorHeaders

[pscustomobject]@{
    health = $health.status
    loginUser = $login.user.email
    business = $business.name
    product = $product.name
    orderId = $order.id
    finalStatus = $updatedOrder.status
    totalAmount = $updatedOrder.totalAmount
    adminPanelRole = $businessDashboard.role
    superAdminPanelRole = $systemOverview.role
    operatorPanelRole = $operatorOverview.role
} | ConvertTo-Json -Depth 5
