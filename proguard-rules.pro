# SandFly's own code finds classes and members by name: the compositor, settings fragments, JSON
# models and the native braille and QuickJS libraries. So it keeps all of its names and members,
# and R8 only optimizes it. The libraries are shrunk and optimized with their own rules.
-dontobfuscate
-keep,allowoptimization class com.google.android.accessibility.** { *; }
-keep,allowoptimization class com.google.android.libraries.accessibility.** { *; }
-keep,allowoptimization class com.google.android.marvin.** { *; }
-keep,allowoptimization class com.google.android.apps.common.** { *; }

# ML Kit creates its components by name, which its own rules don't keep.
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }

# LiteRT-LM's native library calls back into its Java classes by name, and it has no rules.
-keep class com.google.ai.edge.litertlm.** { *; }

# The speech and braille display APIs other apps bind to, which are named after Backtalk.
-keep class fyi.quin.backtalk.** { *; }

# Scripts run in Lua through LuaJava, whose native library calls back into its Java classes by
# name, and scripts with the java permission reach Java classes by name through it.
-keep class party.iroiro.luajava.** { *; }

# Shizuku grants permissions through binder interfaces it finds by name.
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }

# WorkManager, which ML Kit's model downloads use, creates its Room database by name.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# R8's optimizations break ML Kit: its shared Random allows setSeed only while it is being
# constructed, and the optimized check throws in Random's constructor, so the handwriting model
# lookup crashed SandFly. Its own builds are optimized already.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
