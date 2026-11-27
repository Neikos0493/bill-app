package com.neikos0493.jileme.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.biometric.BiometricManager
import com.neikos0493.jileme.domain.GestureHasher
import kotlin.math.hypot

@Composable fun GesturePatternInput(modifier: Modifier=Modifier,onComplete:(List<Int>)->Unit){
    var selected by remember{mutableStateOf(listOf<Int>())};var current by remember{mutableStateOf<Offset?>(null)};var size by remember{mutableStateOf(androidx.compose.ui.unit.IntSize.Zero)}
    val color=MaterialTheme.colorScheme.primary
    fun point(index: Int): Offset {val col=index%3;val row=index/3;return Offset(size.width*(col+.5f)/3,size.height*(row+.5f)/3)}
    fun add(position: Offset){if(size.width==0)return;val nearest=(0..8).minBy{ (point(it)-position).getDistance() };if((point(nearest)-position).getDistance()<size.width/9&&nearest!in selected)selected=selected+nearest}
    Canvas(modifier.aspectRatio(1f).onSizeChanged{size=it}.pointerInput(Unit){awaitEachGesture{val down=awaitPointerEvent().changes.first();selected=emptyList();add(down.position);var dragging=true;while(dragging){val event=awaitPointerEvent();val change=event.changes.first();current=change.position;add(change.position);if(!change.pressed){dragging=false;if(selected.size>=4)onComplete(selected);current=null}}}}){
        selected.zipWithNext().forEach{drawLine(color,point(it.first),point(it.second),strokeWidth=8f)};current?.let{pos->selected.lastOrNull()?.let{drawLine(color,point(it),pos,strokeWidth=6f,alpha=.5f)}}
        (0..8).forEach{val active=it in selected;drawCircle(color,radius=if(active)32f else 20f,center=point(it),alpha=if(active)1f else .45f)}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SecurityScreen(state:AppUiState,viewModel:AppViewModel,onBack:()->Unit){
    var setup by remember{mutableStateOf(false)};var first by remember{mutableStateOf<List<Int>?>(null)};var error by remember{mutableStateOf<String?>(null)};val context=LocalContext.current;val authenticators=BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL;val canRecover=BiometricManager.from(context).canAuthenticate(authenticators)==BiometricManager.BIOMETRIC_SUCCESS
    Scaffold(topBar={TopAppBar(title={Text("安全锁")},navigationIcon={IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回")}})}){padding->Column(Modifier.padding(padding).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        if(!setup){SectionCard("打开 App 时需要验证"){listOf("NONE" to "关闭","BIOMETRIC" to "指纹／系统生物识别","GESTURE" to "手势密码").forEach{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){RadioButton(state.settings.lockType==it.first,{if(it.first=="NONE")viewModel.setLock("NONE")else if(!canRecover)error="请先在系统中设置指纹或锁屏密码"else if(it.first=="GESTURE"){error=null;setup=true}else{error=null;viewModel.setLock(it.first)}});Text(it.second)}};if(state.settings.lockType!="NONE")Text("切到后台后再次打开需要解锁。",style=MaterialTheme.typography.bodySmall);error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}}
        else{Text(if(first==null)"绘制新手势" else "再次绘制以确认",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("至少连接 4 个点");GesturePatternInput(Modifier.fillMaxWidth()){pattern->val initial=first;if(initial==null)first=pattern else if(initial==pattern){viewModel.setLock("GESTURE",GestureHasher.hash(pattern));setup=false;first=null;onBack()}else{error="两次手势不一致，请重新设置";first=null}};error?.let{Text(it,color=MaterialTheme.colorScheme.error)};TextButton({setup=false;first=null}){Text("取消")}}
    }}
}

@Composable fun LockScreen(lockType:String,gestureHash:String,onBiometric:()->Unit,onUnlocked:()->Unit){
    var error by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(lockType){if(lockType=="BIOMETRIC")onBiometric()}
    Column(Modifier.fillMaxSize().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        Text("记了么",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold);Spacer(Modifier.height(12.dp));Text("账单已锁定",color=MaterialTheme.colorScheme.secondary);Spacer(Modifier.height(24.dp))
        if(lockType=="GESTURE"){GesturePatternInput(Modifier.fillMaxWidth()){if(GestureHasher.verify(it,gestureHash))onUnlocked()else error="手势不正确"};error?.let{Text(it,color=MaterialTheme.colorScheme.error)};TextButton(onClick=onBiometric){Text("使用指纹或系统锁屏")}}
        else{Button(onClick=onBiometric){Text("指纹／系统验证解锁")}}
    }
}

