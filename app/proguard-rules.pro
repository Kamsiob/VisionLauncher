# Keep kotlinx.serialization generated serializers for the layout snapshot store.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class io.github.kamsiob.launcher.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.kamsiob.launcher.** {
    kotlinx.serialization.KSerializer serializer(...);
}
