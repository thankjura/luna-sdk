package ru.slie.luna.sdk.command;

import picocli.CommandLine;

@CommandLine.Command(name = "run", description = "Запустить Tomcat с приложением и плагином")
public class RunCommand implements Runnable {
    @Override public void run() {
        System.out.println("run app");
    }
}
