# Goxy resource pack plugin
Require the resource pack only once when joining the server network with multiple servers.

## Config
```yaml
resource-pack:
  url: 'https://example.com/link/to/resourcepack.zip'
  # Optional resourcepack hash, if not provided hash will be computed on plugin load.
  hash: 2849ace6aa689a8c610907a41c03537310949294
  # If it is required to download the resource pack?
  required: true
  # Message to show when the resource pack is sent to player in minimessage format.
  prompt: "This resource pack is <b>required</b> to play on this server. Do you want to download it?"
```

## Video presentation

click me:
[![video presentation](https://img.youtube.com/vi/L41IUiAhQDw/maxresdefault.jpg)](https://youtu.be/L41IUiAhQDw)
