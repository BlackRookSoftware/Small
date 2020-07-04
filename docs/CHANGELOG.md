Small (C) Black Rook Software 2020
==================================
by Matt Tropiano et al. (see AUTHORS.txt)


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
