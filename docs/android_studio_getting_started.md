# Android Studio

In Android Studio, it isn't immediately obvious where you can put your SqlDelight source files. You want to right-click the "java" directory with the blue folder icon and press New -> Package

![New package](images/android_studio_new_package.png)

Select the `sqldelight` directory

![Choose package destination](images/android_studio_package_destination.png)

And create a package to put your SQL files in, like `com.example.myapp.db`:

![Package name](images/android_studio_package_name.png)

You then place your SQL definitions and migrations in that package.

It might end up looking something like the below, with your [migrations](https://cashapp.github.io/sqldelight/migrations/) in a `migrations` package, and your schema database files (for verifying your migrations) in a `schemas` package.

![Result](images/android_studio_heres_one_i_made_earlier.png)