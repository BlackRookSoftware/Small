Small (C) Black Rook Software 2020
==================================
by Matt Tropiano et al. (see AUTHORS.txt)


Changed in 1.5.4
----------------

- `Changed` Exception handlers now search through superclasses for matching handlers.


Changed in 1.5.3
----------------

- `Fixed` InputStreams sent as content are no longer blank (length bug).


Changed in 1.5.2
----------------

- `Fixed` Duplicate singleton component issue. [Issue #2](https://github.com/BlackRookSoftware/Small/issues/2).


Changed in 1.5.1
----------------

- `Changed` CookieParameters throw a better exception if they are not Cookie-typed.
- `Changed` Cookies created via CookieParameter are not automatically added to the response.
- `Changed` Doc fixes.


Changed in 1.5.0
----------------

- `Added` ExceptionHandler.getHandledClass().


Changed in 1.4.2
----------------

- `Fixed` Issue #1 - Classpath Scanning Does Not Work in Java 9 or Higher


Changed in 1.4.1
----------------

- `Fixed` Attempting to instantiate an abstract class as a component yields a better exception. 
- `Changed` SmallConfiguration.getAttribute(String, Object) is now type-parameterized.
- `Changed` SmallEnvironment uses the system class loader for package scan. 


Changed in 1.4.0
----------------

- `Changed` SmallResponse is now an interface, but with a default builder for instances.


Changed in 1.3.0
----------------

- `Added` @BeforeDestruction on @Components for annotation methods to call on context destruction.
- `Changed` @AfterInitialize on @Components can have parameters that match singleton components.
- `Changed` All types on a component are associated with their instance (or many instances).
- `Changed` The SmallEnvironment is now a singleton fetchable during singleton creation.


Changed in 1.2.1
----------------

- `Fixed` It's possible for an encapsulated InputStream to escape closing if an exception occurs
  in an "exit" filter before the payload is rendered. This loophole has now been closed.


Changed in 1.2.0
----------------

- `Added` SmallResponse.dateHeader methods.
- `Added` SmallUtils.encapsulateResponseContent(...)
- `Changed` Handling of Small requests and the Filter/Controller request loop.


Changed in 1.1.0
----------------

- `Added` SmallResponse.
- `Added` Lots of additional methods to SmallResponseUtils.
- `Added` SmallUtils.sendContent().
- `Added` Controllers do not need View/Attachment/Content annotations for methods if the return type is SmallResponse.
- `Changed` Unified the content send.


Changed in 1.0.0
----------------

- Initial release.
