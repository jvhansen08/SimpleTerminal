import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Assign3 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ArrayList<String> commandHistory = new ArrayList<>();
        Timer time = new Timer();
        boolean running = true;

        while (running) {
            System.out.print("[" + System.getProperty("user.dir") + "]: ");
            String rawCommand = scanner.nextLine();
            if (rawCommand.trim().equals("exit")) {
                running = false;
            } else if (rawCommand.strip().equals("")){
                commandHistory.add(rawCommand);
                System.out.println();
            } else {
                executeCommand(rawCommand, commandHistory, time);
            }
        }
    }

    private static void executeCommand(String command, ArrayList<String> cmdHistory, Timer time) {
        cmdHistory.add(command);
        if (command.contains("|")) {
            String[] commands = command.split("[|]");
            String cmd1 = commands[0].trim();
            String cmd2 = commands[1].trim();
            pipe(cmd1.split("\\s+"), cmd2.split("\\s+"), time);
        } else {
            String[] commandSplit = splitCommand(command);
            switch (commandSplit[0].replace("&", "")) {
                case "ptime" -> System.out.printf("Total time in child processes: %.4f seconds\n", time.getTotalTime() / 1000);
                case "history" -> history(cmdHistory);
                case "^" -> repeatCommand(commandSplit, cmdHistory, time);
                case "list" -> list();
                case "cd" -> cd(commandSplit);
                case "mdir" -> mdir(commandSplit);
                case "rdir" -> rdir(commandSplit);
                default -> systemCommand(commandSplit, time);
            }
        }
    }

    private static void cd(String[] command) {
        String currentDir = System.getProperty("user.dir");
        // No filename given
        if (command.length == 1) System.setProperty("user.dir", System.getProperty("user.home"));
        else if (command.length == 2){
            String targetFileName = command[1];
            java.nio.file.Path target = java.nio.file.Paths.get(currentDir, targetFileName);
            try{
                if (target.toFile().isDirectory()) {
                    File file = new File(currentDir);
                    // If in \home, cd .. goes to \ rather than \home\.., same idea for cd .
                    switch (targetFileName) {
                        case ".." -> System.setProperty("user.dir", file.getParent());
                        case "." -> {}
                        default -> System.setProperty("user.dir", target.toString());
                    }
                } else {
                    System.out.printf("Error: %s is not a valid directory\n", targetFileName);
                }
            } catch (NullPointerException e) {
                System.out.println("Error: the desired directory doesn't exist.");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

        //Too many arguments
        } else {
            System.out.println("Invalid usage: cd filename");
        }
    }

    private static void history(ArrayList<String> cmdHistory) {
        for (int i = 0; i < cmdHistory.size(); i++) System.out.printf("%d: %s\n", i + 1, cmdHistory.get(i));
    }

    private static void repeatCommand(String[] command, ArrayList<String> cmdHistory, Timer time) {
        if (command.length == 2) {
            if (isInt(command[1])) {
                int cmdIndex = Integer.parseInt(command[1]) - 1;
                if (cmdIndex >= 0 && cmdIndex < cmdHistory.size() - 1) {
                    // Check for endless recursive command call
                    if (!cmdHistory.get(cmdIndex).contains(Integer.toString(cmdIndex + 1))) executeCommand(cmdHistory.get(cmdIndex), cmdHistory, time);
                    else System.out.println("Recursive call issue: Avoid calling the '^' command if its accompanying number equals its index (i.e. if the history command shows 1. ^ 1, don't try to run the command '^ 1'");
                } else System.out.printf("Out of bounds: Expected an integer between 1 and %d, got %d instead\n", cmdHistory.size() - 1, cmdIndex + 1);
            } else System.out.printf("Expected an integer, instead got '%s'\n", command[1]);
        } else System.out.println("Invalid number of arguments: ^ invocation number");
    }

    private static void list() {
        String currentDir = System.getProperty("user.dir");
        File fileDir = new File(currentDir);
        try {
            for (File file : Objects.requireNonNull(fileDir.listFiles())) {
                StringBuilder output = new StringBuilder();
                SimpleDateFormat date = new SimpleDateFormat("MMM dd, yyyy HH:mm");
                String dateFormatted = date.format(file.lastModified());
                long size = file.isDirectory() ? 0 : file.length();
                output.append(file.isDirectory() ? 'd' : '-')
                        .append(file.canRead() ? 'r' : '-')
                        .append(file.canWrite() ? 'w' : '-')
                        .append(file.canExecute() ? 'x' : '-')
                        .append(String.format("%10d", size))
                        .append(" ").append(dateFormatted)
                        .append(" ").append(file.getName())
                ;
                System.out.println(output);
            }
        } catch (NullPointerException e) {
            System.out.println();
        }

    }

    private static void mdir(String[] command) {
        if (command.length == 2) {
            String currentDir = System.getProperty("user.dir");
            java.nio.file.Path proposed = java.nio.file.Paths.get(currentDir, command[1]);
            if (proposed.toFile().isDirectory()) System.out.println("This directory already exists");
            else if (proposed.toFile().isFile()) System.out.println("There is already a file with this name");
            else {
                try{
                    java.nio.file.Files.createDirectory(proposed);
                } catch (Exception e) {
                    System.out.printf("An unexpected error occurred: %s\n", e.getMessage());
                }
            }
        } else System.out.println("Invalid number of arguments: mdir directoryName");

    }

    private static void rdir(String[] command) {
        if (command.length == 2) {
            String currentDir = System.getProperty("user.dir");
            java.nio.file.Path proposed = java.nio.file.Paths.get(currentDir, command[1]);
            if (!proposed.toFile().isDirectory()) System.out.println("This directory does not exist");
            else {
                try{
                    java.nio.file.Files.delete(proposed);
                } catch (Exception e) {
                    System.out.printf("An unexpected error occurred: %s\n", e.getMessage());
                }
            }
        } else System.out.println("Invalid number of arguments: rdir directoryName");
    }

    private static void systemCommand(String[] command, Timer time) {
        try {
            boolean wait = !command[command.length - 1].equals("&");
            command[command.length - 1] = command[command.length - 1].replace("&", "");
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            double startTime = System.currentTimeMillis();
            Process p = pb.start();
            if (wait) {
                p.waitFor();
            }
            time.addTime(System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            System.out.printf("Command '%s' not recognized\n", command[0]);
            System.out.println(e.getMessage());
        }
    }

    private static void pipe(String[] cmd1, String[] cmd2, Timer time) {
        boolean wait = !cmd2[cmd2.length - 1].equals("&");
        cmd2[cmd2.length - 1] = cmd2[cmd2.length - 1].replace("&", "");
        ProcessBuilder pb1 = new ProcessBuilder(cmd1);
        ProcessBuilder pb2 = new ProcessBuilder(cmd2);

        pb1.redirectInput(ProcessBuilder.Redirect.INHERIT);
        pb2.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        try {
            double startTime = System.currentTimeMillis();
            Process p1 = pb1.start();
            Process p2 = pb2.start();

            java.io.InputStream in = p1.getInputStream();
            java.io.OutputStream out = p2.getOutputStream();

            int data;
            while ((data = in.read()) != -1) {
                out.write(data);
            }
            out.flush();
            out.close();
            if (wait){
                p1.waitFor();
                p2.waitFor();
                time.addTime(System.currentTimeMillis() - startTime);
            }
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.out.println("Invalid usage of piping.");
        }
    }
    public static String[] splitCommand(String command) {
        java.util.List<String> matchList = new java.util.ArrayList<>();

        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(command);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group());
            }
        }
        return matchList.toArray(new String[matchList.size()]);
    }

    private static boolean isInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
