# Github Language Statistics

The application scans all repositories in the configured organization and creates a "report" about all used languages.
It's based on REST Github API: 

- https://docs.github.com/en/rest/repos/repos#list-organization-repositories - retrieves all repositories in a single organization
- https://docs.github.com/en/rest/repos/repos#list-repository-languages - returns a collection of used languages and the number of bytes

## Building

- Application is based on Java 19 and can be built using Maven (Java 19 needs to be installed locally):

```
mvn clean package
```

However, the preferable approach is to use Docker 

```
docker build . -t github-language-statistics:1.0
```

## Execution

Start the app from command-line (Java 19 needs to be installed locally):

```
java --module-path target/classes:target/lib --enable-preview -m pbouda.github.lang/pbouda.github.lang.Application
```

or preferably using Docker:

```
docker run -p 8080:8080 github-language-statistics:1.0
```

## Access the endpoint 

From a browser:

http://localhost:8080/statistics/languages

or command-line:

```
curl http://localhost:8080/statistics/languages
```

## Expected output

The application automatically scales to 2 decimal places and rounds using HALF_UP mode, therefore, some languages have "0" ratio,
but they are still included in the output because the language appeared in one of the repositories at least.

```
{
  "C#": 0.02,
  "Procfile": 0.00,
  "C": 0.00,
  "Makefile": 0.00,
  "Go": 0.00,
  "HTML": 0.03,
  "Svelte": 0.00,
  "TypeScript": 0.48,
  "Shell": 0.00,
  "JavaScript": 0.20,
  "Lua": 0.00,
  "Ruby": 0.14,
  "Python": 0.00,
  "PowerShell": 0.00,
  "Java": 0.12,
  "CSS": 0.00,
  "C++": 0.00,
  "Vue": 0.00,
  "Logos": 0.00,
  "Dockerfile": 0.00,
  "CoffeeScript": 0.00,
  "Batchfile": 0.00,
  "Gherkin": 0.00,
  "ASP.NET": 0.00,
  "Roff": 0.00,
  "Nix": 0.00,
  "TSQL": 0.00
}
```

## Design decisions

- The project contains some Preview and Incubating features from OpenJDK, the reason why I included them into Project is that 
I wanted to try it out and have been looking for a suitable project to give them try. And I used this to one :)! 
  - https://openjdk.org/jeps/425 : `Virtual Threads`
  - https://openjdk.org/jeps/428 : `Structured Concurrency`
- I focused on minimizing of external dependencies in the application. It's always hard to find the balance between bringing in 
dependencies and writing some piece of code based only on OpenJDK. e.g.:
  - I didn't use an external library for an HTTP retry mechanism and used my own retry mechanism based purely on JDK HTTP Client 
  and CompletableFuture - https://gist.github.com/petrbouda/92647b243eac71b089eb4fb2cfa90bf2
  - The requirement for the time scheduling was very simple - "once per day" - therefore, I didn't use any Spring extension 
  such as @Scheduled annotation driven by a more powerful CRON expression and used a simple `ScheduledExecutorService`.
    - In general, with SpringBoot I like to code the way that minimize spreading Spring's code, annotations and configuration across
    the application code and trying to keep it in @Configuration classes and combining pure Java classes to avoid making beans from every 
    instance (only to be able to put annotations there), proxies and crippling thread's stack-traces. 
    It keeps tread-dumps, flamegraphs readable and makes the code testable with simple JUnit and Mockito.

## Some more work would be needed

- Definitely, not all rainy cases were covered and HTTP API does not provide any detailed message saying what went wrong
(only Spring predefined messages e.g. 404) and definitely, covering these cases by more rainy tests.
- Better parallelism in a case of paging the repositories. Current Github API does not provide (I haven't found at least) 
any good/predictable way to download multiple pages in parallel. We could blindly guess and prefetch some pages using additional 
HTTP Requests and speculates that they contain some data.

## Obstacles

- The project contains a pre-generated API Github Token, otherwise, it's really not possible to effectively work with it. 
Everyone can generate the token and pass it into the configuration `application.properties` file to be able to make more 
than tens of requests to Github REST API.
- Current API Token won't be active longer than one week (honestly, not sure how it actually works, I mean Github tokens...)

```
Account -> Settings -> Developer Settings -> Personal Access Tokens -> Token (classic)
```