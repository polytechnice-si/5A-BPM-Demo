# Demonstrating Flowable

  * author: Sebastien Mosser
  * revision: 10.17
  * Strongly inspired by the _awesome_ documentation provided by Flowable: 
    * [http://www.flowable.org/docs/userguide/index.html](http://www.flowable.org/docs/userguide/index.html)
  
  
## My Very First Business Process

We consider a simple business process to support vacation approval. 

<p align="center">
  <img src="https://raw.githubusercontent.com/polytechnice-si/5A-BPM-Demo/master/docs/process.png" />
</p>

__English version__: an employee starts a process instance by sending a request containing her name, the number of days requested and a reason associated to this request. A manager can then validate a pending request. If the request is legit, the system transfers the request to the Enterprise Information System which log the vacations. If the manager rejects the request, a rejection email is sent to the employee. The BPMN implementation is available in the file [`holiday-request.bpmn20.xml`](https://github.com/polytechnice-si/5A-BPM-Demo/blob/master/src/main/resources/holiday-request.bpmn20.xml).
 
 
## Creating an Embedded App

In this section, we will embed a process engine in a regular Java application. We use Maven to support the project definition. 


### Project definition

The [`pom.xml`](https://github.com/polytechnice-si/5A-BPM-Demo/blob/master/pom.xml) basically loads the process engine (Flowable) and an in-memory database engine (h2)

```xml
<dependency>
  <groupId>org.flowable</groupId>
  <artifactId>flowable-engine</artifactId>
  <version>6.1.2</version>
</dependency>
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <version>1.3.176</version>
</dependency>
```

We will use the classical Maven structure for source code: `src/main/java` for java classes and `src/main/resources` to store resource files (_e.g._, logging properties and process definitions). 
 
### Application structure

The application is simple. We start by initialising the engine and deploying the holiday process. Then, the user can chose if she wants to act as a _manager_ or an _employee_, and acts consequently. When the program is terminated, it will display some metrics about the remaining process instances.

```java
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
``` 

### Engine initialisation

The engine initialisation is a purely technical code. Based on a `ProcessConfiguration` bound to an H2 embedded database, we create a `ProcessEngine`.

```java
private static void initializeEngine() {
  ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
    .setJdbcUrl("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=-1")
    .setJdbcUsername("sa")
    .setJdbcPassword("")
    .setJdbcDriver("org.h2.Driver")
    .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
  processEngine = cfg.buildProcessEngine();
}
```

### Deploying a process definition

We request the `RepositoryService`, and deploy the process definition as a resource (the file is located in `src/main/resources`).

```java
private static void deployResourceAsProcess(String resourceName) {
  RepositoryService repositoryService = processEngine.getRepositoryService();
  repositoryService.createDeployment()
    .addClasspathResource(resourceName)
    .deploy();
}
```

### Instantiating a Process


When acting as an _Employee_, we trigger a new process instantiation. We read from the CLI the needed information to trigger a request, and then asks the `RuntimeService` to create an instance of the `holidayRequest` process using these data.

```
private static void beAnEmployee() {
  // Reading employee name, numberOfHolidays and description from the command line
  
  RuntimeService runtimeService = processEngine.getRuntimeService();
  Map<String, Object> variables = new HashMap<String, Object>();
  variables.put("employee", employee);
  variables.put("nrOfHolidays", nrOfHolidays);
  variables.put("description", description);
  ProcessInstance inst = runtimeService.startProcessInstanceByKey("holidayRequest", variables);
  System.out.println("Process started, #" + inst.getId());
}
```

### Retrieving Pending User Tasks

We asks the `TaskService` to send the list of tasks waiting for an action from the _managers_ group. A task is assigned to a given group using the `flowable:candidateGroups="managers"` attribute in the BPMN definition.

```java
  TaskService taskService = processEngine.getTaskService();
  List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("managers").list();
  System.out.println("You have " + tasks.size() + " tasks:");
  for (int i=0; i<tasks.size(); i++) {
    System.out.println(i + ") " + tasks.get(i).getName() + " #" + tasks.get(i).getProcessInstanceId());
  }
```

### Interact With a Task

Considering a given task, we can access to the variables associated to it (here the holiday request). We complete the task by calling the `complete` method on the `TaskService`, with the task identifier and the variable produced by this human task (here the approval or rejection of the request).

```java
  Task task = tasks.get(taskIndex);
  Map<String, Object> processVariables = taskService.getVariables(task.getId());
  
  System.out.println(processVariables.get("employee") + " wants " +
    processVariables.get("nrOfHolidays") + " of holidays. Do you approve this? (y/n)");
  boolean approved = scanner.nextLine().toLowerCase().equals("y");
  
  Map<String, Object> variables = new HashMap<String, Object>();
  variables.put("approved", approved);
  taskService.complete(task.getId(), variables);
```

### Implementing service task

By using the `flowable:class` attribute, one can declare the Java class to be triggered to process the task automatically. In our example, we simply print something on the standard output.

```java
public class SendRejectionMail implements JavaDelegate {

  public void execute(DelegateExecution execution) {
    System.out.println("Sending rejection email for " + execution.getVariable("employee"));
  }
  
}
```

### Compiling and Running the process
 
 
The POM is configured to start the `HolidayRequest` class by default. We simply asks maven to compile (`package`) and then execute the program (`exec:java`). 
```
azrael:5A-BPM-Demo mosser$ mvn -q package exec:java
``` 

## Using the default REST server

We rely on the Docker image provided by Flowable. To start the REST server, simply start a container bound to the official image. The default user is named `kermit`, and the password os also `kermit`.

```
$ docker run -p8080:8080 flowable/flowable-rest
```

To check engine availability, send a GET request to a basic route and check the response.

```
$ curl --user kermit:kermit http://localhost:8080/flowable-rest/service/management/engine 
```

### Interacting with the server

The server requests JSON data, and will answer JSON data. Consider the `json_pp` tool to pretty print the responses received from the server.

To deploy a process, we use the `-F` parameter to send a file content as the request body to the deployments resource.

```
$ curl --user kermit:kermit -F "file=@./src/main/resources/holiday-request.bpmn20.xml" http://localhost:8080/flowable-rest/service/repository/deployments
```


To list the available processes:

```
$ curl --user kermit:kermit http://localhost:8080/flowable-rest/service/repository/process-definitions
```

Instantiating a process:

```
$ curl --user kermit:kermit -H "Content-Type: application/json" -X POST -d '{ "processDefinitionKey":"holidayRequest", "variables": [ { "name":"employee", "value": "John Doe" }, { "name":"nrOfHolidays", "value": 7 }]}' http://localhost:8080/flowable-rest/service/runtime/process-instances
```

Retrieving pending tasks for _managers_

```
$ curl --user kermit:kermit -H "Content-Type: application/json" -X POST -d '{ "candidateGroup" : "managers" }' http://localhost:8080/flowable-rest/service/query/tasks
```

Completing a task (where `ID` is replaced by a valid task identifier):

```
$ curl --user kermit:kermit -H "Content-Type: application/json" -X POST -d '{ "action" : "complete", "variables" : [ { "name" : "approved", "value" : true} ]  }' http://localhost:8080/flowable-rest/service/runtime/tasks/ID
```

The task ends in error, as the custom class are not available in the server.

## Specialising the docker image


We simply need to create a new docker image that contains our custom class in the execution context of the process engine.
  