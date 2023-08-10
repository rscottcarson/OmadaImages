# Omada Images
Technical assignment for Omada Health

Developed on Android Studio Giraffe | 2022.3.1

## Approach and libraries used
For this technical project I used an MVVM architecture and utilized Compose for the View layer. All
state is hoisted out of the View layer and is managed by the ViewModel. I wrote a `SimplePager` class
that handles fetching new data when needed during scroll. It also caches pages of data and is 
configurable to find the right balance between memory management, network request management and UI 
responsiveness.

**Libraries**:
- Kotlinx Serialization - JSON deserialization
- Coil - async image loading with decent compose support
- Ktor - Networking
- Dagger/Hilt - Dependency injection

## Known Issues
The following is a list of known issues with the application:

- **"Janky" paging**: The paging implementation sometimes causes elements to jump one spot forward or 
back when a new page is loaded. Unfortunately this is due to a bug/behavior in the Flickr API that 
results in pages sometimes returning fewer results per page than requested. i.e. I have the 
`SimplePager` configured to request 120 (divisible by 3, great for our gridview) but often times the 
API only returns 118 or 119. This causes our data to not be perfect multiples of 3 which results in
`LazyVerticalGrid` shifting keyed items. (See: [Flickr issue thread](https://www.flickr.com/groups/51035612836@N01/discuss/72157666364892360/))
  - Possible solutions would be add filler (blank) objects to the grid or request overlapping pages
and merge the data. I did not pursue either of these because it did not have a large enough impact 
on the experience using the application and my time was limited.

## Improvements
The following is a list of improvements I would make to the app given more time:

- **Unit test `SimplePager`**: I did not get to the point of being able to write unit tests for the 
`SimplePager` class. This would help ensure paging integrity.
- **Store Flickr API key centrally**: Store the API key in an obfuscated, central location, NOT smack 
in the middle of each API call.
- **Build a second screen for the Photo Detail requirement**: I believe a full screen UI could look 
better and more consistent between photos.
- **Pager API**: The generic `SimplePager` API is still a little too catered towards the apps 
specific use case
- **Paging error handling**: With regards to the paging implementation itself, internally the 
`SimplePager` just ensures the `PageOperation` (in this case api calls) goes well. The `Page` output 
does not take into account internal errors, unknown states, etc. This would be especially important 
if the pager was extended to also utilize a local datastore.
- **Network error handling**: Currently all network errors are handled the same. It would be useful
to separate them out to allow differentiation and surface relevant ones to the user. I made the 
assumption that a network was available, so the app will most likely crash if it is run with no 
network.
- **Loading images UI**: While images are loading there could be a better placeholder other than the 
generic android icon background.
