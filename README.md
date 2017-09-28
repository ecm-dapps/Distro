# Distro

This will be an ethereum browser for __Android__.

It exposes ethereum at http://localhost:8545, using web3J.

To test on your mobile site:
* add Web3.js to your site preferably [from the web3js github site](https://github.com/ethereum/web3.js/blob/develop/dist/web3.min.js)
* intialize web3 i.e. `web3 = new Web3(new Web3.providers.HttpProvider("http://localhost:8545");`
* on the Distro Browser, once you navigate to the site, most of `web3js` methods will be available
  * except, the filter methods
  * the personal methods
  
The apk is [here on github](https://github.com/iamalvin/Distro/blob/master/app/app-release.apk), test it, tell me what you think.

I'm reachable at alvin.browser3@gmail.com

This is mostly useful for developers, not user ready yet

## License
__MPL 2.0__
