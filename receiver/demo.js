// External namespace for cast specific javascript library
var cast = window.cast || {};

// Anonymous namespace
(function() {
  'use strict';

  ChromecastDemo.PROTOCOL = 'ar.com.creativa77.chromecast.demo';

  function ChromecastDemo() {
    this.mChannelHandler =
        new cast.receiver.ChannelHandler('ChromecastDemoDebug');
    this.mChannelHandler.addEventListener(
        cast.receiver.Channel.EventType.MESSAGE,
        this.onMessage.bind(this));
    this.mChannelHandler.addEventListener(
        cast.receiver.Channel.EventType.OPEN,
        this.onChannelOpened.bind(this));
    this.mChannelHandler.addEventListener(
        cast.receiver.Channel.EventType.CLOSED,
        this.onChannelClosed.bind(this));
  }

  ChromecastDemo.prototype = {

    /**
     * Channel opened event; checks number of open channels.
     * @param {event} event the channel open event.
     */
    onChannelOpened: function(event) {
      console.log('onChannelOpened. Total number of channels: ' +
          this.mChannelHandler.getChannels().length);
    },

    /**
     * Channel closed event; if all devices are disconnected,
     * closes the application.
     * @param {event} event the channel close event.
     */
    onChannelClosed: function(event) {
      console.log('onChannelClosed. Total number of channels: ' +
          this.mChannelHandler.getChannels().length);

      if (this.mChannelHandler.getChannels().length == 0) {
        window.close();
      }
    },

    /**
     * Message received event; determines event message and command, and
     * choose function to call based on them.
     * @param {event} event the event to be processed.
     */
    onMessage: function(event) {
      var message = event.message;
      //var channel = event.target;
      console.log('********onMessage********' + JSON.stringify(message));

      if (message.request) {
        $('#title').text(message.request);
        this.broadcast({ response: 'title changed.'})
      } else {
        var err_msg = 'Invalid message command: ' + message.command;
        cast.log.error(err_msg);
        this.sendError(channel, err_msg);
      }
    },

    sendError: function(channel, errorMessage) {
      channel.send({ error: errorMessage });
    },

    /**
     * Broadcasts a message to all of this object's known channels.
     * @param {Object|string} message the message to broadcast.
     */
    broadcast: function(message) {
      this.mChannelHandler.getChannels().forEach(
        function(channel) {
          channel.send(message);
        });
    },

  };

  // Exposes public functions and APIs
  cast.ChromecastDemo = ChromecastDemo;
})();
