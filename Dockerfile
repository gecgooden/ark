FROM openjdk:8

MAINTAINER George Gooden <gecgooden@gmail.com>

ENV MAVEN_VERSION 3.6.3

RUN mkdir -p /usr/share/maven \
  && curl -fsSL http://apache.osuosl.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz \
    | tar -xzC /usr/share/maven --strip-components=1 \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven

RUN mkdir -p /usr/src/app /usr/target/
WORKDIR /usr/src/app

RUN apt-get update && apt-get install -y build-essential libxml2 libxml2-dev libcurl4 libcurl4-gnutls-dev zlib1g zlib1g-dev libbz2-1.0 libbz2-dev libssl-dev

ADD ark-common/src/main/native/madeline /usr/src/app/ark-common/src/main/native/madeline

RUN cd /usr/src/app/ark-common/src/main/native/madeline && \
	sed -i "s/java-6-openjdk-amd64/java-8-openjdk-amd64/g" Makefile && \
	make clean && \
	make && \
	mv /usr/src/app/ark-common/src/main/native/madeline/libmadeline.so /usr/target/

# Below is a pretty hacky approach to cache the downloading of the 
# maven repository so that we won't have to download it every build,
# although if you change any of the following pom files, it will be
# run again.
ADD pom.xml pom.xml 
ADD ark-admin/pom.xml ark-admin/pom.xml
ADD ark-common/pom.xml ark-common/pom.xml
ADD ark-container/pom.xml ark-container/pom.xml
ADD ark-disease/pom.xml ark-disease/pom.xml
ADD ark-genomics/pom.xml ark-genomics/pom.xml
ADD ark-lims/pom.xml ark-lims/pom.xml
ADD ark-phenotypic/pom.xml ark-phenotypic/pom.xml
ADD ark-registry/pom.xml ark-registry/pom.xml
ADD ark-report/pom.xml ark-report/pom.xml
ADD ark-study/pom.xml ark-study/pom.xml
ADD ark-user-account/pom.xml ark-user-account/pom.xml
ADD ark-work-tracking/pom.xml ark-work-tracking/pom.xml
ADD 3rdParty 3rdParty

ENV ARKVERSION 1.2b.3
RUN mkdir -p $HOME/.m2/repository/au/org/theark/admin/ark-admin/${ARKVERSION} \
	$HOME/.m2/repository/au/org/theark/core/ark-common/${ARKVERSION} \
	$HOME/.m2/repository/au/org/theark/disease/ark-disease/${ARKVERSION} \
	$HOME/.m2/repository/au/org/theark/genomics/ark-genomics/${ARKVERSION} \
	$HOME/.m2/repository/au/org/theark/lims/ark-lims/${ARKVERSION} \
	$HOME/.m2/repository/au/org/theark/phenotypic/ark-phenotypic/${ARKVERSION} \
	$HOME/.m2/repository/au/org/theark/registry/ark-registry/${ARKVERSION} \
	$HOME/.m2/repository/au/org/theark/report/ark-report/${ARKVERSION} \
	$HOME/.m2/repository/au/org/theark/study/ark-study/${ARKVERSION} \
	$HOME/.m2/repository/au/org/theark/worktracking/ark-work-tracking/${ARKVERSION}

RUN touch $HOME/.m2/repository/au/org/theark/admin/ark-admin/${ARKVERSION}/ark-admin-${ARKVERSION}.jar  \
	$HOME/.m2/repository/au/org/theark/admin/ark-admin/${ARKVERSION}/ark-admin-${ARKVERSION}.pom \
	$HOME/.m2/repository/au/org/theark/core/ark-common/${ARKVERSION}/ark-common-${ARKVERSION}-tests.jar \
	$HOME/.m2/repository/au/org/theark/core/ark-common/${ARKVERSION}/ark-common-${ARKVERSION}.jar \
	$HOME/.m2/repository/au/org/theark/core/ark-common/${ARKVERSION}/ark-common-${ARKVERSION}.pom \
	$HOME/.m2/repository/au/org/theark/disease/ark-disease/${ARKVERSION}/ark-disease-${ARKVERSION}.jar \
	$HOME/.m2/repository/au/org/theark/disease/ark-disease/${ARKVERSION}/ark-disease-${ARKVERSION}.pom \
	$HOME/.m2/repository/au/org/theark/genomics/ark-genomics/${ARKVERSION}/ark-genomics-${ARKVERSION}.jar \
	$HOME/.m2/repository/au/org/theark/genomics/ark-genomics/${ARKVERSION}/ark-genomics-${ARKVERSION}.pom \
	$HOME/.m2/repository/au/org/theark/lims/ark-lims/${ARKVERSION}/ark-lims-${ARKVERSION}.jar \
	$HOME/.m2/repository/au/org/theark/lims/ark-lims/${ARKVERSION}/ark-lims-${ARKVERSION}.pom \
	$HOME/.m2/repository/au/org/theark/phenotypic/ark-phenotypic/${ARKVERSION}/ark-phenotypic-${ARKVERSION}.jar \
	$HOME/.m2/repository/au/org/theark/phenotypic/ark-phenotypic/${ARKVERSION}/ark-phenotypic-${ARKVERSION}.pom \
	$HOME/.m2/repository/au/org/theark/registry/ark-registry/${ARKVERSION}/ark-registry-${ARKVERSION}.jar \
	$HOME/.m2/repository/au/org/theark/registry/ark-registry/${ARKVERSION}/ark-registry-${ARKVERSION}.pom \
	$HOME/.m2/repository/au/org/theark/report/ark-report/${ARKVERSION}/ark-report-${ARKVERSION}.jar \
	$HOME/.m2/repository/au/org/theark/report/ark-report/${ARKVERSION}/ark-report-${ARKVERSION}.pom \
	$HOME/.m2/repository/au/org/theark/study/ark-study/${ARKVERSION}/ark-study-${ARKVERSION}.jar \
	$HOME/.m2/repository/au/org/theark/study/ark-study/${ARKVERSION}/ark-study-${ARKVERSION}.pom \
	$HOME/.m2/repository/au/org/theark/worktracking/ark-work-tracking/${ARKVERSION}/ark-work-tracking-${ARKVERSION}.jar \
	$HOME/.m2/repository/au/org/theark/worktracking/ark-work-tracking/${ARKVERSION}/ark-work-tracking-${ARKVERSION}.pom 

RUN mvn initialize
RUN mvn dependency:resolve
# End of hack

ADD ark-user-account /usr/src/app/ark-user-account

#Replace with reading from env_file
ADD docker/env_file docker/env_file
RUN cat docker/env_file >> ~/.bashrc

ADD docker/base/ark-user-account-config.sh docker/base/ark-user-account-config.sh
RUN chmod +x docker/base/ark-user-account-config.sh
RUN docker/base/ark-user-account-config.sh

RUN mvn -f ark-user-account/pom.xml assembly:assembly

RUN cp ark-user-account/target/ark-user-account-1.0.0-jar-with-dependencies.jar /usr/target/ark-user-account.jar

# Add everything else
ADD . /usr/src/app/

RUN chmod +x docker/base/ark-container-config.sh
RUN docker/base/ark-container-config.sh

RUN mvn package

RUN cp ark-container/target/ark.war /usr/target/ark.war

#Overriding the CMD from mvn so that we can cleanly exit on run
CMD /bin/true

#So that we can mount these volumes into the other containers, to pull out the pieces needed
VOLUME /usr/target
