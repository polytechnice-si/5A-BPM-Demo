package org.flowable;

import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class HolidayRequest {


    private static final String PROCESS_DEFINITION_KEY = "holidayRequest";

    private static ProcessEngine processEngine;
    private static ROLE currentRole;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        initializeEngine();
        deployResourceAsProcess("holiday-request.bpmn20.xml");
        while( choseRole() ) {
            switch (currentRole) {
                case MANAGER:  beAManager(); break;
                case EMPLOYEE: beAnEmployee(); break;
            }
        }
        metrics();
        ProcessEngines.destroy();
    }

    private enum ROLE { EMPLOYEE, MANAGER; }

    private static void initializeEngine() {
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=-1")
                .setJdbcUsername("sa")
                .setJdbcPassword("")
                .setJdbcDriver("org.h2.Driver")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        processEngine = cfg.buildProcessEngine();
    }

    private static void deployResourceAsProcess(String resourceName) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        repositoryService.createDeployment()
                .addClasspathResource(resourceName)
                .deploy();
    }

    private static boolean choseRole() {
        try {
            System.out.println("Are you an EMPLOYEE or a MANAGER? (or STOP)");
            currentRole = ROLE.valueOf(scanner.nextLine());
            return true;
        } catch (Exception e) { return false; }
    }

    private static void beAnEmployee() {
        System.out.println("/**\n * Acting as an EMPLOYEE\n **/");
        Scanner scanner= new Scanner(System.in);
        System.out.println("Who are you?");
        String employee = scanner.nextLine();
        System.out.println("How many holidays do you want to request?");
        Integer nrOfHolidays = Integer.valueOf(scanner.nextLine());
        System.out.println("Why do you need them?");
        String description = scanner.nextLine();

        RuntimeService runtimeService = processEngine.getRuntimeService();
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("employee", employee);
        variables.put("nrOfHolidays", nrOfHolidays);
        variables.put("description", description);
        ProcessInstance inst = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, variables);
        System.out.println("Process started, #" + inst.getId());
    }

    private static void beAManager() {
        System.out.println("\n\n/**\n * Acting as a team MANAGER\n **/");
        TaskService taskService = processEngine.getTaskService();
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("managers").list();
        if (tasks.isEmpty()) {
            System.out.println("Nothing to do!");
            return;
        }

        System.out.println("You have " + tasks.size() + " tasks:");
        for (int i=0; i<tasks.size(); i++) {
            System.out.println((i+1) + ") " + tasks.get(i).getName() + " #" + tasks.get(i).getProcessInstanceId());
        }
        System.out.println("Which task would you like to complete?");
        int taskIndex = Integer.valueOf(scanner.nextLine());
        Task task = tasks.get(taskIndex - 1);
        Map<String, Object> processVariables = taskService.getVariables(task.getId());
        System.out.println(processVariables.get("employee") + " wants " +
                processVariables.get("nrOfHolidays") + " of holidays. Do you approve this? (y/n)");
        boolean approved = scanner.nextLine().toLowerCase().equals("y");
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("approved", approved);
        taskService.complete(task.getId(), variables);
    }


    private static void metrics() {
        System.out.println("\n\n/**\n * System Metrics\n **/\n");
        HistoryService historyService = processEngine.getHistoryService();
        List<ProcessInstance> instances = processEngine.getRuntimeService()
                .createProcessInstanceQuery()
                .list();

        for(ProcessInstance processInstance: instances) {
            System.out.println("Metrics for process instance" + processInstance.getId());
            List<HistoricActivityInstance> activities =
                    historyService.createHistoricActivityInstanceQuery()
                            .processInstanceId(processInstance.getId())
                            .orderByHistoricActivityInstanceEndTime().asc()
                            .list();
            for (HistoricActivityInstance activity : activities) {
                System.out.println(activity.getActivityId() + " took "
                        + activity.getDurationInMillis() + " milliseconds");
            }
            System.out.println("");
        }
    }

}