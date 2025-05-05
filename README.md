# langchain4j-ollama-tool-invocation

https://github.com/paulhilliar/langchain4j-ollama-tool-invocation

Really simple demo of Langchain4J tool integration with Ollama running a local LLM 

1. Install Ollama
2. Run the model: ollama run mistral-small:latest  (it's about a 14GB download)
3. Set project JDK to Java 21
4. Run ChapbotApp

If you are changing the spec (descriptive text) for a tool or the system then remember to restart the model in Ollama
with "/bye" then "ollama run" again. 


This started from a prompt to Google AI studio of: 
build me a Java + maven chatbot project that uses langchain4j and llama3:8b-instruct.  
The chatbot should have a tool that gives the current time in a country's capital when you pass in the country code eg GBR, FRA, USA.  
Include instructions on how to run ollama.  
The project should run on a M3 pro

If you get this error
java: java.lang.ExceptionInInitializerError
com.sun.tools.javac.code.TypeTag :: UNKNOWN

then you need to set the IntelliJ project to run Java 21.
File, project structure, sdk...

