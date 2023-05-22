## Overview
This application will instantiate a simple terminal capable of running simple shell commands, as shown in the table below:
| Command | Parameters | Sample Usage | Result    |
|---------|---------|---------|---------|
| ptime   | N\A        | ptime | Shows time spent executing child processes |
| list    | N\A        | list | Similar to 'ls -l' in linux |
| cd      | FILENAME   | cd .., cd src | Change directory |
| mdir    | FILENAME   | mdir new_directory | Create a directory if it doesn't already exist |
| rdir    | FILENAME   | rdir old_directory | Remove a directory if it doesn't already exist |
| history | N\A        | history | Shows a numbered list of previously run commands |
| ^       | INDEX      | ^ 8 | Runs a command from history at given index |
| exit | N/A | exit | Terminate program |

Unrecognized commands will be sent to parent shell for execution.

## Build Instructions
This application uses gradle to handle the build process. In the root directory, run the following commands:
* gradle build
* java -jar build/libs/Assign3.jar
The resulting prompt should look like [\<current directory\>]: