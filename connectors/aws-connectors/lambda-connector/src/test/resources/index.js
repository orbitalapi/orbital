exports.handler = async (event) => {
   const streamingProviders = [
      { name: "Netflix", pricePerMonth: 9.99 },
      { name: "Disney Plus", pricePerMonth: 7.99 },
      { name: "Now TV", pricePerMonth: 13.99 },
      { name: "Hulu", pricePerMonth: 8.99 }
   ];

   let selectedStreamingProvider;
   if (event.filmId === 1) {
      selectedStreamingProvider =  streamingProviders[0]
   } else if (event.filmId === 2) {
      selectedStreamingProvider =  streamingProviders[1]
   } else {
      const streamingProviderIndex = Math.floor(Math.random() * streamingProviders.length);
      selectedStreamingProvider = streamingProviders[streamingProviderIndex];
   }


   const response = {
      statusCode: 200,
      body: selectedStreamingProvider,
   };
   return response;
};
