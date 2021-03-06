package com.intellij.plugin.buck.command;
import java.io.*;

/**
 * Created by longma on 6/18/15.
 */
public class BuckCommandController {
    private File _envDir;
    private String _projectName;
    private BuckCommandUtils.ProcessStatus _currentStatus;

    public BuckCommandController(String envDir, String projectName) {
        this._envDir = new File(envDir);
        this._projectName = projectName;
        _currentStatus = BuckCommandUtils.ProcessStatus.NONE;
    }

    public void setEnvDir(File newDir) {
        this._envDir = newDir;
    }

    public void setProjectName(String newProjectName) {
        this._projectName = newProjectName;
    }

    public File getEnvDir() {
        return this._envDir;
    }

    public String getProjectName() {
        return this._projectName;
    }


    public BuckCommandUtils.ProcessStatus getCurrentStatus() {
        return _currentStatus;
    }

    public void setCurrentStatus(BuckCommandUtils.ProcessStatus s) {
        _currentStatus = s;
    }


    public void executeBuckCommand(BuckCommandUtils.CommandType type){
        String[] command = BuckCommandUtils.getCommand(type, this._projectName);
        switch (type) {
            case COMMAND_INSTALL:
                executeInstallCommand(command, _envDir);
                break;
            case COMMAND_BUILD:
                executeBuildCommand(command, _envDir);
                break;
            case COMMAND_TEST:
                executeTestCommand(command, _envDir);
                break;
            default:
                break;
        }
    }

    private void executeInstallCommand(String[] command, File envDir){
        BuckCommandProcessMonitor commandProcessMonitor = new BuckCommandProcessMonitor("'buck install'");
        new Thread(commandProcessMonitor).start();
        _currentStatus = BuckCommandUtils.ProcessStatus.STARTED;
        try {
            Process p = Runtime.getRuntime().exec(command, null, envDir);
            BuckCommandUtils.outputLog("buck install...");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String output;
            while ((output = bufferedReader.readLine()) != null) {
                BuckCommandUtils.outputLog(output);
                //check status
                updateStatus(output, commandProcessMonitor);
                switch (_currentStatus) {
                    case NONE:
                        break;
                    case PARSING_BUCK_FILES_FINISHED:
                        String numJobDone = BuckCommandUtils.getNumJobDone(output);
                        if (numJobDone != null) {
                            BuckCommandUtils.outputLog(numJobDone + " DONE!");
                        }
                        break;
                    case BUILDING_FINISHED:
                        break;
                    case INSTALLING_FINISHED:
                        BuckCommandUtils.outputLog("buck install finished!");
                        //kill monitor
                        commandProcessMonitor.stopBoadcast();
                        break;
                    default:
                        break;
                }
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void executeBuildCommand(String[] command, File envDir){
        BuckCommandProcessMonitor commandProcessMonitor = new BuckCommandProcessMonitor("'buck build'");
        Thread monitor = new Thread(commandProcessMonitor);
        monitor.start();
        try {
            Process p = Runtime.getRuntime().exec(command, null, envDir);
            BuckCommandUtils.outputLog("buck build...");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String output;
            while ((output = bufferedReader.readLine()) != null) {
                BuckCommandUtils.outputLog(output);
            }
            bufferedReader.close();
            BuckCommandUtils.outputLog("buck build finshed!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void executeTestCommand(String[] command, File envDir){
        BuckCommandProcessMonitor commandProcessMonitor = new BuckCommandProcessMonitor("'buck test'");
        Thread monitor = new Thread(commandProcessMonitor);
        monitor.start();
        try {
            Process p = Runtime.getRuntime().exec(command, null, envDir);
            BuckCommandUtils.outputLog("buck test...");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String output;
            while ((output = bufferedReader.readLine()) != null) {
                BuckCommandUtils.outputLog(output);
            }
            bufferedReader.close();
            BuckCommandUtils.outputLog("buck test finshed!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateStatus(String output, BuckCommandProcessMonitor monitor){
        if (BuckCommandUtils.contains(output, "PARSING BUCK FILES...FINISHED")) {
            _currentStatus = BuckCommandUtils.ProcessStatus.PARSING_BUCK_FILES_FINISHED;
        }
        if (BuckCommandUtils.contains(output, "BUILDING...FINISHED")) {
            _currentStatus = BuckCommandUtils.ProcessStatus.BUILDING_FINISHED;
        }
        if (BuckCommandUtils.contains(output, "INSTALLING...FINISHED")) {
            _currentStatus = BuckCommandUtils.ProcessStatus.INSTALLING_FINISHED;
        }
        monitor.updateStatus(_currentStatus);
    }
}
