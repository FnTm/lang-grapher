Code Language Grapher
===

This is a fairly simple tool to graph the change in language use in a single, local, git repository.

It accomplishes this by using the [git-linguist](https://github.com/github/linguist) tool developed by Github.

Usage
---
 - Install [git-linguist](https://github.com/github/linguist) by running
```
$ gem install github-linguist
```
 - Run

 ```
 $ ./gradlew build
 ```
 - Run the tool

 ```
 $ ./gradlew run -Pargs="/Users/example/dev/lang-graph master 460 1490313600"
 ```

 The args above have to be in order, and have the following semantics:

  1. The absolute path to the repository in questions (`/Users/example/dev/lang-graph` in the example)
  1. The branch of that repo to use (`master` in the example)
  1. Number of days to look back (`460` in the example)
  1. (Optional) The epoch of the day when to stop looking (`1490313600` in the example). This was introduced to allow stopping on a particular day when checking.

 - Output will be located in the `/output` directory in both jpg and json formats
 
 Sample output:
  ![Sample output](https://user-images.githubusercontent.com/1011549/29772101-d10322c6-8bff-11e7-899d-3694e52936c3.jpg)
  
