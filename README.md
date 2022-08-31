# Working with Containers

Openshift workshop to review containers creation, deployment and security constraints.

## Parent Container

Parent container initial version overview:

```sh
# Build parent container
podman build -t parent --format docker parent-container

# Inspect image (6 layers) -> 247MB
podman images
podman inspect parent
podman image inspect parent --format "{{len .RootFS.Layers}}"

# Run parent container
podman run --name parent -d -p 80:80 parent
```

Update parent container to reduce number of layers and size:

```Dockerfile
FROM registry.access.redhat.com/ubi8/ubi:8.0

MAINTAINER Carlos Lopez <calopezb@redhat.com>

# DocumentRoot for Apache
ENV DOCROOT=/var/www/html

# Httpd installation
RUN yum install --disableplugin=subscription-manager httpd -y && \
    yum clean all --disableplugin=subscription-manager -y && \
    echo "Hello from the parent container!" > ${DOCROOT}/index.html && \
    rm -rf /run/httpd && mkdir /run/httpd

# Allows child images to inject their own content into DocumentRoot
ONBUILD COPY src/ ${DOCROOT}/

# Httpd server port
EXPOSE 80

# Run as the root user
USER root

# Launch httpd
CMD /usr/sbin/httpd -DFOREGROUND
```

Test new image:

```sh
# Build parent container
podman build -t parent --format docker parent-container

# Inspect image (3 layers) -> 237MB
podman images
podman inspect parent
podman image inspect parent --format "{{len .RootFS.Layers}}"
```

## Child Container

Build, test and push to Quay

```sh
# Build child image
podman build -t child child-container

# Review size and layers
podman images
podman image inspect child --format "{{len .RootFS.Layers}}"

# Login into Quay
podman login quay.io

# Tag and push image
podman tag localhost/child quay.io/calopezb/child                  
podman push quay.io/calopezb/child
```

Deploy into OCP

```sh
# Create new project
oc new-project container-images

# Import image (fails)
oc import-image child --from quay.io/calopezb/child --confirm

# Create secret for quayio
oc create secret generic quayio --from-file .dockerconfigjson=/Users/calopezb/.config/containers/auth.json --type kubernetes.io/dockerconfigjson
oc secret link default quayio --for pull
oc import-image child --from quay.io/calopezb/child --confirm
oc get istag

# Create app
oc new-app --name child-app -i child
oc get pods -w

# Review error
oc logs child-app-xxxxxx

# Cleanup app
oc delete all -l app=child-app
```

Update child container:

```Dockerfile
FROM localhost/parent

EXPOSE 8080

RUN sed -i "s/Listen 80/Listen 8080/g" /etc/httpd/conf/httpd.conf && \
    sed -i "s/#ServerName www.example.com:80/ServerName 0.0.0.0:8080/g" /etc/httpd/conf/httpd.conf && \
    chgrp -R 0 /var/log/httpd /var/run/httpd && \
    chmod -R g=u /var/log/httpd /var/run/httpd

USER 1001
```

Build and push into quay. Redeploy into OCP
```sh
# Build image
podman build -t child child-container

# Tag and push image
podman tag localhost/child quay.io/calopezb/child                  
podman push quay.io/calopezb/child

# Update tag
oc import-image child
oc get istag

# Recreate application
oc new-app --name child-app -i child
oc get pods -w

# Expose and check app
oc expose svc child-app --port "8080-tcp"
oc get route
curl child-app-container-images.apps.XXXX
```

## Deploy Applications

Create namespace:

```sh
oc new-project deploy-apps
```

Use a pod file to run an image:

```sh
# Create pod
oc apply -f hello.yaml

# Delete pod
oc delete pod hello
```

Create needed resources to deploy application:

```sh
# Create application
oc new-app --name hello --docker-image openshift/hello-openshift --allow-missing-images

# Review created resources
oc get all

# Expose application and test
oc expose svc hello
curl hello-deploy-apps.apps.XXXX

# Clean up environment (show labels before)
oc get deploy hello -o yaml
oc get deploy --show-labels
oc delete all -l app=hello
oc get all
```

Deploy quarkus application (s2i)

```









