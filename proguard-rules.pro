# ============================================================
# kotlinx.serialization
# ============================================================
-dontwarn kotlinx.serialization.**
-dontnote kotlinx.serialization.**
-dontnote kotlinx.serialization.internal.PlatformKt
-dontnote kotlinx.serialization.AnnotationsKt

-keepattributes *Annotation*, InnerClasses

# Keep all @Serializable classes and their companions
-if @kotlinx.serialization.Serializable class **
-keep class <1> {
    static <1>$Companion Companion;
}

-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}

-keepclasseswithmembers class **$$serializer {
    static **$$serializer INSTANCE;
}

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class kotlinx.serialization.descriptors.** { *; }

# App-specific serializable classes
-keep class data.AppSettings { *; }
-keep class data.AppSettings$$serializer { *; }

# ============================================================
# kotlinx.coroutines
# ============================================================
-dontwarn kotlinx.coroutines.**
-dontnote kotlinx.coroutines.**

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Coroutines use volatile fields internally — keep them to avoid crashes
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepclassmembernames class java.util.concurrent.atomic.AtomicReferenceFieldUpdater {
    volatile <fields>;
}

# ============================================================
# androidx.lifecycle (ViewModel)
# ============================================================
-dontwarn androidx.lifecycle.**
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ============================================================
# Koin (dependency injection)
# ============================================================
-dontwarn org.koin.**
-dontnote org.koin.**
-keep class org.koin.** { *; }
-keepclassmembers class org.koin.** { *; }

# ============================================================
# vlcj (video playback)
# ============================================================
-dontwarn uk.co.caprica.**
-keep class uk.co.caprica.** { *; }
-keepclassmembers class uk.co.caprica.** { *; }

# JNA (Java Native Access) — vlcj uses JNA for all native bindings.
# Without this, ProGuard notes every JNA descriptor class referenced
# from a kept vlcj entry point but not itself kept.
-dontwarn com.sun.jna.**
-keep class com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.** { *; }

# com.sun.awt.AWTUtilities — used by vlcj overlay component, not present on all JDKs
-dontwarn com.sun.awt.**

-dontwarn sun.misc.**

# ============================================================
# Deskit (file chooser dialogs)
# ============================================================
-dontwarn deskit.**
-keep class deskit.** { *; }

# ============================================================
# Compose resources (generated class)
# ============================================================
-dontwarn org.jetbrains.compose.resources.**
-keep class org.jetbrains.compose.resources.** { *; }
-keep class simplevideoeditor.resources.** { *; }

# ============================================================
# Application classes
# ============================================================

# Entry point
-keep class SimpleVideoEditor { *; }

# Data layer
-keep class data.** { *; }

# Services (FFmpeg, VideoPlayer) — instantiated directly
-keep class services.** { *; }

# ViewModels — instantiated by Koin via reflection
-keep class viewmodel.** { *; }

# Koin module definitions
-keep class di.** { *; }

# ============================================================
# JVM / Swing internals (suppress known warnings)
# ============================================================
-dontwarn sun.font.CFont
-dontwarn sun.swing.SwingUtilities2$AATextInfo
-dontwarn net.miginfocom.swing.MigLayout
-dontwarn org.slf4j.**
-dontnote org.slf4j.**
-dontnote META-INF.**

# ============================================================
# Service loader (keep META-INF entries for coroutines dispatchers etc.)
# ============================================================
-keepdirectories META-INF/services/
-keep class META-INF.services.** { *; }
