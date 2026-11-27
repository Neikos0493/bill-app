package com.neikos0493.jileme

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.neikos0493.jileme.domain.Money
import com.neikos0493.jileme.ui.*

class MainActivity : FragmentActivity() {
    private var unlocked=false
    private var currentLockType="NONE"
    private var biometricPrompt: BiometricPrompt?=null
    private var unlockCallback:(()->Unit)?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel:AppViewModel=viewModel();val state by viewModel.state.collectAsStateWithLifecycle();val nav=rememberNavController()
            var showBudget by remember{mutableStateOf(false)};var lockNonce by remember{mutableIntStateOf(0)}
            currentLockType=state.settings.lockType
            val snackbar=remember{SnackbarHostState()}
            LaunchedEffect(state.message){state.message?.let{snackbar.showSnackbar(it);viewModel.consumeMessage()}}
            LaunchedEffect(state.loading){if(!state.loading && state.settings.lastUpdateCheck<System.currentTimeMillis()-12*3600_000) viewModel.checkUpdate(false)}
            JilemeTheme(state.settings.themeMode){
                if(state.settings.lockType!="NONE"&&!unlocked){
                    LockScreen(state.settings.lockType,state.settings.gestureHash,onBiometric={unlockCallback={unlocked=true;lockNonce++};showBiometric()},onUnlocked={unlocked=true;lockNonce++})
                }else{
                    val mainRoutes=listOf("home","transactions","stats","settings")
                    val destination=nav.currentBackStackEntryAsState().value?.destination?.route
                    Scaffold(
                        snackbarHost={SnackbarHost(snackbar)},
                        bottomBar={if(destination in mainRoutes)NavigationBar{listOf(Triple("home","首页",Icons.Default.Home),Triple("transactions","账单",Icons.Default.List),Triple("stats","统计",Icons.Default.Star),Triple("settings","我的",Icons.Default.Settings)).forEach{item->NavigationBarItem(selected=destination==item.first,onClick={nav.navigate(item.first){popUpTo("home"){saveState=true};launchSingleTop=true;restoreState=true}},icon={Icon(item.third,item.second)},label={Text(item.second)})}}},
                        floatingActionButton={if(destination in mainRoutes)FloatingActionButton({nav.navigate("editor/0")}){Icon(Icons.Default.Add,"记一笔")}}
                    ){padding->NavHost(nav,"home",Modifier.padding(padding)){
                        composable("home"){HomeScreen(state,{nav.navigate("editor/0")},{nav.navigate("pending")},{nav.navigate("editor/$it")},{showBudget=true},{nav.navigate("accounts")})}
                        composable("transactions"){TransactionsScreen(state,false,{nav.navigate("editor/0")},{nav.navigate("editor/$it")})}
                        composable("pending"){TransactionsScreen(state,true,{nav.navigate("editor/0")},{nav.navigate("editor/$it")})}
                        composable("stats"){StatsScreen(state)}
                        composable("settings"){SettingsScreen(state,viewModel,{nav.navigate("accounts")},{nav.navigate("categories")},{nav.navigate("import")},{nav.navigate("security")})}
                        composable("editor/{id}",arguments=listOf(navArgument("id"){type=NavType.LongType})){EditorScreen(state,it.arguments?.getLong("id")?:0,viewModel){nav.popBackStack()}}
                        composable("accounts"){AccountsScreen(state,viewModel){nav.popBackStack()}}
                        composable("categories"){CategoriesScreen(state,viewModel){nav.popBackStack()}}
                        composable("import"){ImportScreen(state,viewModel){nav.popBackStack()}}
                        composable("security"){SecurityScreen(state,viewModel){nav.popBackStack()}}
                    }}
                    if(showBudget){var value by remember{mutableStateOf(Money.input(state.settings.monthlyBudgetMinor))};AlertDialog(onDismissRequest={showBudget=false},title={Text("月度预算")},text={MoneyField(value,{value=it})},confirmButton={TextButton({Money.parse(value)?.let(viewModel::setBudget);showBudget=false}){Text("保存")}},dismissButton={TextButton({showBudget=false}){Text("取消")}})}
                }
            }
        }
    }

    private fun showBiometric(){
        val authenticators=BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if(BiometricManager.from(this).canAuthenticate(authenticators)!=BiometricManager.BIOMETRIC_SUCCESS){Toast.makeText(this,"设备未设置可用的指纹或锁屏",Toast.LENGTH_LONG).show();return}
        val prompt=BiometricPrompt(this,ContextCompat.getMainExecutor(this),object:BiometricPrompt.AuthenticationCallback(){override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult){unlockCallback?.invoke()};override fun onAuthenticationError(errorCode:Int,errString:CharSequence){if(errorCode!=BiometricPrompt.ERROR_USER_CANCELED&&errorCode!=BiometricPrompt.ERROR_NEGATIVE_BUTTON)Toast.makeText(this@MainActivity,errString,Toast.LENGTH_SHORT).show()}})
        biometricPrompt=prompt;prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("解锁记了么").setSubtitle("验证身份后查看账单").setAllowedAuthenticators(authenticators).build())
    }
    override fun onStop(){super.onStop();if(currentLockType!="NONE")unlocked=false}
}
