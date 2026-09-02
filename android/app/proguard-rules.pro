-keep class com.notaskflow.app.NotaskFlowApplication { *; }
-keep class com.notaskflow.app.MainActivity { *; }

-keep class **JsonAdapter { *; }
-keep @com.squareup.moshi.JsonClass class com.notaskflow.** { *; }

-keepclassmembers enum com.notaskflow.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
