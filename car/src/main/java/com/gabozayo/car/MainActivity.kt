package com.gabozayo.car

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import com.gabozayo.car.ui.CarScreen
import com.gabozayo.car.ui.CarTheme

class MainActivity : ComponentActivity() {
    /** 화면이 닫혀도 유지되는 앱 전체의 엔진을 써요 */
    private val vm: CarViewModel get() = (application as CarApp).engine

    /** FUSED_LOCATION 은 실행 중에 허용을 받아야 하는 권한(dangerous)이에요 */
    private val locationPermission = "pleos.car.permission.FUSED_LOCATION"
    private val askLocation = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.startVehicle() else vm.vehicleDenied()
        syncPerms()
    }
    private val drivePermission = "pleos.car.permission.CAR_DRIVE"
    private val askDrive = registerForActivityResult(ActivityResultContracts.RequestPermission()) { syncPerms(); vm.refreshVehicle() }
    private val askNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) { syncPerms() }
    /** 처음 실행할 때 필요한 권한(위치·차량 데이터·알림)을 한 번에 요청해요 */
    private val askAll = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r[locationPermission] == true) vm.startVehicle() else if (r.containsKey(locationPermission)) vm.vehicleDenied()
        syncPerms(); vm.refreshVehicle()
    }
    private fun syncPerms() = vm.setPermissions(
        checkSelfPermission(locationPermission) == PackageManager.PERMISSION_GRANTED,
        checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        checkSelfPermission(drivePermission) == PackageManager.PERMISSION_GRANTED,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CarTheme {
                CarScreen(vm, onAskLocation = { askLocation.launch(locationPermission) }, onAskNotif = { askNotif.launch(android.Manifest.permission.POST_NOTIFICATIONS) }, onAskDrive = { askDrive.launch(drivePermission) })
            }
        }
        syncPerms()
        // 화면을 닫아도 휴대폰에서 온 목적지를 받도록 앱을 살려 둬요
        runCatching { startForegroundService(Intent(this, KeepAliveService::class.java)) }
        if (checkSelfPermission(locationPermission) == PackageManager.PERMISSION_GRANTED) vm.startVehicle()
        val missing = listOf(locationPermission, drivePermission, android.Manifest.permission.POST_NOTIFICATIONS)
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        // 권한 창은 앱을 처음 켤 때 한 번만 띄워요. (거절했거나 화면이 다시 만들어질 때마다 뜨지 않게)
        // 나중에 허용하려면 옵션 화면의 "허용하기" 를 누르면 돼요.
        val prefs = getSharedPreferences("gabozayo_perm", MODE_PRIVATE)
        if (missing.isNotEmpty() && savedInstanceState == null && !prefs.getBoolean("asked", false)) {
            prefs.edit().putBoolean("asked", true).apply()
            askAll.launch(missing.toTypedArray())
        }
    }

    /** 설정이나 adb 로 권한을 허용하고 돌아오면 바로 연결해요 */
    override fun onResume() {
        super.onResume()
        syncPerms()
        if (checkSelfPermission(locationPermission) == PackageManager.PERMISSION_GRANTED) vm.startVehicle()
    }
}
