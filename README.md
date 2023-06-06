# Cats Sandbox

This repository contains an empty Scala/Cats project
for use in [Spacecat](https://spacecat.io) training courses.

Licensed [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).

## Getting Started

1. To use this repo you'll need to install a Java 11+ compatible JDK.
   We recommend using [Sdkman](https://sdkman.io/) to do this:

   ```bash
   sdk list java
   sdk install java <VERSION_TAKEN_FROM_THE_LIST>
   ```

2. Clone this repository to a directory on your hard drive:

   ```bash
   git clone https://github.com/spacecatio/cats-sandbox.git
   cd cats-sandbox
   ```

3. Run the `sbt.sh` script (or use your own locally installed SBT):

   ```bash
   ./sbt.sh
   # Lots of output here. The first run will take a while.
   # You'll see an SBT prompt as follows:
   sbt:sandbox> run
   ```

4. Type `run` at the SBT prompt to run the sample app:

   ```bash
   sbt:sandbox> run
   # Lots of output here. The first run will take a while.
   # You should finally see the following output:
   Hello world!
   ```

5. Type `exit` to quit the SBT prompt when you're done:

   ```bash
   sbt:sandbox> exit
   ```

## Editing Using Visual Studio Code

If you don't have a particular preference for a Scala editor or IDE,
we recommend you use [Visual Studio Code](https://code.visualstudio.com/) and a Linux or macOS terminal.

Ideally you should have the `code` command [set up in your terminal](https://code.visualstudio.com/docs/editor/command-line#_code-is-not-recognized-as-an-internal-or-external-command), in which case:

```bash
cd cats-sandbox
code .
```

If you don't have the command installed, you can open the code from the file menu:

1. Choose _File Menu > New Window_
2. Choose _File Menu > Open Folder..._ and select the repo root folder

We recommend using your terminal to compile and run your code.

You can optionally install the [Metals](https://github.com/scalameta/metals-vscode) plugin
to add IDE-like features for editing Scala in VSCode.
However, please take the time to get this working correctly _before_ your course.

## Editing Using IntelliJ IDEA

If you prefer to edit Scala code using [IntelliJ IDEA](https://www.jetbrains.com/idea):

1. Make sure you have the [Scala plugin](https://www.jetbrains.com/help/idea/discover-intellij-idea-for-scala.html) installed
2. Choose _File Menu > Open..._ and select the `build.sbt` file from the repo root folder
3. Choose "Open as Project"
