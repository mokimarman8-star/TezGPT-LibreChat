# TezGPT native Android release rules.

# Keep runtime annotations and generic signatures used by AndroidX and JSON models.
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations,Signature,InnerClasses,EnclosingMethod

# Preserve the native launcher and WorkManager worker entry points.
-keep public class com.tezgpt.app.MainActivity { public <methods>; }
-keep public class com.tezgpt.app.agents.AgentSyncWorker { public <init>(android.content.Context,androidx.work.WorkerParameters); public <methods>; }

# Preserve model constructors and fields used by reflective JSON adapters.
-keep class com.tezgpt.app.agents.AgentRun { *; }
-keep class com.tezgpt.app.tools.ToolDefinition { *; }

# Preserve AndroidX FileProvider metadata and provider behavior.
-keep class androidx.core.content.FileProvider { *; }

# Keep source and line information for actionable crash reports without retaining verbose names.
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Do not treat missing optional annotations as fatal during shrinking.
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
