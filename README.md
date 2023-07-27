# Jenkins Shared Libraries

In Jenkins, a shared library is a way to store commonly used code(reusable code), such as scripts or functions, that can be used by different 
Jenkins pipelines. 

Instead of writing the same code again and again in multiple pipelines, you can create a shared library and use it in all the pipelines
that need it. This can make your code more organized and easier to maintain. 

Think of it like a library of books, Instead of buying the same book over and over again, you can borrow it from the library whenever you need it.



## Advantages

- Standarization of Pipelines
- Reduce duplication of code
- Easy onboarding of new applications, projects or teams
- One place to fix issues with the shared or common code
- Code Maintainence 
- Reduce the risk of errors

## Directory structure
The directory structure of a Shared Library repository is as follows:

<img width="424" alt="Directory_structure" src="https://github.com/RavikiranSavai/Jenkins-Shared-Library/assets/76962621/13f3bca6-18ba-43b1-bafe-265f9e6a505d">

**vars**: This directory holds all the global shared library code that can be called from a pipeline.
It has all the library files with a .groovy extension.

**src**: It is added to the class path during very script compilation. We can add custom groovy code to extend our shared library code.

**resources**: All the non-groovy files required for your pipelines can be managed in this folder.

