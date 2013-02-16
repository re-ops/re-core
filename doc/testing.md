# Testing


## Unit testing

Celestial currently uses [expectations](https://github.com/jaycfields/expectations) for unit testing. 

```bash 
 $ lein expectations
```

## Integration

Celestial has a list of integration tests agains proxmox and redis, its really easy to set them up:

For redis:
```bash 
 $ git clone https://github.com/narkisr/redis-sandbox
 $ bundle install 
 $ librarian-puppet install 
 $ vagrant up
```

Now run the tests:

```bash
 $ lein test :redis
```

For proxmox:

```bash 
 # download proxmox appliance, add it to virtualbox and start it up 
 $ wget http://ubuntuone.com/7NkQdEY5QOJ4anyXOQjig9
```

Now run:

```bash 
  $ lein test :proxmox
```
