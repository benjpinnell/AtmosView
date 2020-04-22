# Build Instructions for AtmosView

The AtmosView project has been updated to use [Bazel](https://bazel.build/) to build the code. To
help manage the version of Bazel that should be used, the project leverages
[Bazelisk](https://github.com/bazelbuild/bazelisk) The version of Bazel that will be downloaded and
used for building is specified in [.bazelversion](/.bazelversion).

## Install Bazelisk

### On the Mac

#### Install Homebrew

The easiest way to install Bazelisk is to use [Homebrew](https://brew.sh/). Check if Homebrew is
installed by running:

```sh
brew -v
```
If you receive a "command not found" error, then install Homebrew by running the following:

```sh
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install.sh)"
```

Be sure to open another Terminal window to pick up the changes and be able to access the `brew`
command.

#### Install Bazelisk

Once Homebrew is installed run the following to install Bazelisk.

```sh
brew install bazelisk
```

## How to ...

The following instructions assume that you are in the root directory of the `AtmosView` project.
For instance, if you cloned this repository to `$HOME/code/AtmosView`, then you should run the
following to get to the root of the project:

```sh
cd $HOME/code/AtmosView
```

Also, note that all of the run and test commands listed below will build the project, if necessary.

### How to Run AtmosView

```sh
bazelisk run src/main/java/ca/ubc/cs/sanchom/atmosview
```

### How to Execute the Tests

```sh
bazelisk test //...
```
