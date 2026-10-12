# IFT3913 — Tâche 2 

| Nom complet | Identifiant GitHub |
|---|---|
| Dhaiby, Tala     | TalaDhaiby     |
| Elshahawy, Layla | laylaelshahawy |

## Classe sélectionnée

Nous avons choisi `org.apache.tika.utils.DateUtils`, dans le module
`tika-core`. Cette classe permet notamment d’analyser et de formater
des dates.

Elle possède déjà des tests, mais sa couverture initiale n’est pas
complète et certains mutants survivent. Ces résultats justifient
notre choix pour la génération de tests supplémentaires.

Commit de référence :
`a2d75c2c10563b261148e9bafe4046832b33c1a6`.

## Environnement et outils

- Windows 11
- Java 22 ; compilation Maven ciblant Java 17
- Maven 3.9.11
- ChatUniTest 2.1.1
- PIT 1.18.2
- Plugin PIT pour JUnit 5 : 1.2.2
- Ollama 0.35.1
- Modèle ouvert exécuté localement : `qwen2.5-coder:1.5b`
- RAM : 16 Go

ChatUniTest et PIT ont été ajoutés comme plugins Maven dans
`tika-core/pom.xml`.

## Résultats initiaux avec les tests originaux

Les tests existants se trouvent notamment dans
`tika-core/src/test/java/org/apache/tika/metadata/TestMetadata.java`.

Ils exercent `DateUtils` indirectement : `Metadata.getDate()` appelle
`parseDate()`, qui appelle `DateUtils.tryToParse()`.

Une analyse PIT a été réalisée avec ces tests originaux, sans les
modifier. PIT mesure une couverture de 37 lignes sur 38, soit environ
97 %. Sur les 23 mutants générés, 18 sont tués et 5 survivent.
Le score de mutation initial est donc de 18/23, soit environ 78 %.

Le rapport initial est conservé dans
`rapports/tests-originaux/pit-reports/`.

## Génération avec ChatUniTest

ChatUniTest utilise le modèle `qwen2.5-coder:1.5b`, exécuté localement
avec Ollama. Le nom `codeqwen:v1.5-chat` utilisé dans la configuration
est un alias local de ce modèle, créé pour être accepté par ChatUniTest.

Le modèle de 7 milliards de paramètres a d’abord été essayé, mais son
chargement a échoué par manque de mémoire disponible. Le modèle de
1,5 milliard de paramètres a ensuite été utilisé.

La configuration du plugin est la suivante :

```xml
<plugin>
  <groupId>io.github.zju-aces-ise</groupId>
  <artifactId>chatunitest-maven-plugin</artifactId>
  <version>2.1.1</version>
  <configuration>
    <apiKeys>ollama</apiKeys>
    <model>codeqwen:v1.5-chat</model>
    <url>http://localhost:11434/v1/chat/completions</url>
    <testNumber>3</testNumber>
    <maxRounds>3</maxRounds>
    <thread>false</thread>
    <testOutput>${project.basedir}/chatunitest</testOutput>
    <tmpOutput>${project.build.directory}/chatunitest-info</tmpOutput>
  </configuration>
</plugin>
```

### Résultat de la génération

La génération a duré environ 38 minutes. Elle a traité les quatre
méthodes publiques de la classe, en comptant les deux surcharges de
`formatDate` séparément. Trois candidats par méthode ont été tentés,
avec au maximum trois tours par candidat.

Les journaux indiquent 30 échecs de compilation et 6 échecs
d’extraction du code. Aucun candidat n’a été validé automatiquement.

L’historique et les messages d’erreur ont été produits sous
`tika-core/target/chatunitest-info/tika-parent/tika-core/`.
Le journal de génération est conservé dans
`rapports/chatunitest-generation-3.log`.

Le candidat `DateUtils_tryToParse_6_2_Test` a été récupéré dans
l’historique de ChatUniTest, puis intégré manuellement dans :

`tika-core/src/test/java/org/apache/tika/utils/DateUtils_tryToParse_6_2_Test.java`.

Son code avant correction est conservé dans
`rapports/DateUtils_tryToParse_6_2_Test-original.txt`.

### Compilation, exécution et corrections

Le candidat récupéré compile après ajout des dépendances de test
`mockito-core` et `mockito-junit-jupiter`. Lors de sa première
exécution, trois tests réussissent et cinq échouent, sans erreur
d’exécution. Le contrôle du style du code, Checkstyle, a été désactivé
pour cette première évaluation.

Les cinq échecs révèlent des oracles incorrects. Quatre supposent que
le parseur rejette toute chaîne mal formée, alors qu’il peut analyser
un préfixe de la chaîne. Le cinquième attend minuit dans le fuseau
local au lieu de midi UTC pour une date sans heure.
Trois tests utilisent également la même entrée et sont redondants.

Cinq oracles ont été corrigés manuellement. L’ordre attendu/obtenu
des deux assertions `assertEquals` originales a également été corrigé.
Ces changements concernent les huit tests du même candidat.

Les imports avec `*` ont été remplacés par des imports explicites,
et l’extension Mockito inutilisée a été retirée. Les huit tests
passent ensuite avec Checkstyle activé.

Des interventions manuelles ont donc été nécessaires pour rendre
les tests générés fonctionnels.

### Documentation des tests générés

ChatUniTest a produit un candidat contenant huit méthodes de test pour `DateUtils.tryToParse()`. Les tests ont été récupérés dans l'historique de génération, intégrés au projet, puis corrigés manuellement lorsque leurs oracles ne correspondaient pas au comportement réel de la méthode.

Les scénarios couvrent principalement les entrées nulles, les chaînes trop courtes, les dates valides, les dates accompagnées d'une heure ainsi que les entrées contenant des informations supplémentaires ou invalides.

**Objectifs et choix des données**

| Scénario | Intention | Motivation des données | Oracle |
|---|---|---|---|
| Entrée nulle | Vérifier le comportement lorsque la date est absente. | `null` représente une entrée non fournie. | Le résultat attendu est `null`. |
| Chaîne trop courte | Vérifier que le parseur rejette une entrée insuffisante. | `2023` ne représente pas une date complète. | Le résultat attendu est `null`. |
| Date valide | Vérifier la reconnaissance d'une date au format ISO. | `2023-04-15` représente une date valide sans heure explicite. | Comparaison avec une date attendue construite à partir d'un `Instant`. |
| Date valide avec heure | Vérifier l'interprétation d'une date et d'une heure. | `2023-04-15T12:34:56` contient les composantes d'une date et d'une heure. | Comparaison avec une valeur `Date` attendue. |
| Date contenant une heure invalide | Observer la gestion d'une entrée dont l'heure est mal formée. | L'entrée contient une date reconnaissable suivie d'une composante horaire incorrecte. | Vérification du comportement réel du parseur, qui peut accepter un préfixe valide. |
| Date avec des informations horaires supplémentaires | Vérifier comment le parseur traite une chaîne contenant plus d'informations qu'un format reconnu. | Une chaîne avec plusieurs composantes horaires permet d'examiner la tolérance du parseur. | Comparaison avec le résultat correspondant au format effectivement reconnu. |
| Date invalide sans information de fuseau horaire | Vérifier l'interprétation d'une date malgré une chaîne mal formée. | `2023-04-15-12:34:56` permet notamment de vérifier le comportement associé à midi UTC. | Comparaison avec une date attendue explicite. |
| Autre entrée mal formée | Vérifier le comportement sur une entrée contenant une structure de date incorrecte. | Les composantes supplémentaires permettent de tester la robustesse du parseur. | Comparaison avec le résultat réellement prévu par l'implémentation. |

**Limites de la génération**

Certains tests générés reposaient sur des hypothèses incorrectes, notamment l'idée que toute chaîne mal formée devait être rejetée. D'autres utilisaient des assertions redondantes. Les oracles ont donc été examinés et corrigés avant l'analyse de mutation.

Ces résultats montrent que la génération automatique peut proposer des scénarios utiles, mais que les assertions produites doivent être validées par rapport au comportement réel et à la spécification du logiciel.

### Comparaison des oracles

Les tests originaux de `TestMetadata` vérifient notamment des dates
attendues précises, la conversion des fuseaux horaires et la gestion
des millisecondes. Leurs oracles sont spécifiques aux comportements
de Tika.

Les tests générés vérifient correctement les entrées nulles et trop
courtes, mais cinq oracles reposent sur des hypothèses incorrectes.
Les trois tests redondants n’apportent pas trois comportements
différents à vérifier.

Pour les dates avec heure, les tests générés utilisent
`SimpleDateFormat` pour calculer la valeur attendue. Cet oracle est
moins indépendant, puisque `DateUtils` utilise aussi ce mécanisme.

Pour les cas correspondant à midi UTC, les corrections utilisent une
date attendue explicite construite avec `Instant`, ce qui évite la
confusion avec minuit dans le fuseau local.

## Analyse de mutation avec les tests générés corrigés

Avec les tests originaux et les tests générés corrigés, PIT mesure
une couverture de 38 lignes sur 38, soit 100 %. Il détecte 22 mutants
sur 23, soit un score de mutation d’environ 96 %. Un mutant survit
donc encore.

Ce résultat concerne les tests générés après correction manuelle,
et non le code produit directement par ChatUniTest.

Le rapport est conservé dans
`rapports/originaux-et-ia-corrigee/pit-reports/`.

### Quatre mutants supplémentaires détectés

Dans le rapport « IA corrigée », PIT attribue la détection des quatre
mutants supplémentaires au test `testTryToParseInvalidDateWithoutTime`.
Ce test analyse `2023-04-15-12:34:56` et attend le 15 avril 2023 à
midi UTC.

À la ligne 63, le mutant inverse la condition `timezone != null`.
La création des formats tente alors d’appliquer un fuseau `null`,
ce qui provoque une exception pendant l’initialisation de `DateUtils`.

À la ligne 64, le mutant supprime l’appel à `setTimeZone`. Le format
utilise alors le fuseau local au lieu du fuseau prévu. Dans notre
environnement, la date obtenue ne correspond plus à midi UTC,
ce qui fait échouer l’assertion.

À la ligne 66, le mutant remplace le résultat de `createDateFormat`
par `null`. La liste contient donc des formats `null`, dont
l’utilisation provoque une exception lors de l’analyse.

À la ligne 138, le mutant remplace le résultat de `loadDateFormats`
par une liste vide. Aucun format n’est essayé et l’analyse retourne
`null`, alors que le test attend une date précise.

Les mutations des lignes 63 et 66 sont donc détectées grâce à une
exception, tandis que celles des lignes 64 et 138 sont détectées
par l’assertion sur le résultat attendu.

## Mutant restant et test manuel supplémentaire

Le mutant de la ligne 151 remplace `length() < 6` par `length() <= 6`.
Il survit aux tests originaux et aux tests générés corrigés.

Le test manuel `testTryToParseExactlySixCharacters`, dans
`DateUtilsManualTest`, vérifie qu’une date analysable de six caractères
est acceptée. L’entrée `"23-1-1"` a été choisie pour tester exactement
cette frontière de longueur. L’oracle `assertNotNull` vérifie que
l’analyse retourne une date : les formats utilisés par le parseur
original acceptent cette entrée avec des champs courts.

Avec la condition modifiée, le mutant retourne immédiatement `null`
pour cette entrée. Le test échoue donc et permet de détecter le mutant.

Le test est enregistré dans :

`tika-core/src/test/java/org/apache/tika/utils/DateUtilsManualTest.java`.

## Résultats finaux

Après ajout du test manuel, PIT mesure une couverture de 38 lignes
sur 38 et détecte les 23 mutants générés. Le score final est de 100 %,
contre environ 78 % avec les tests originaux et environ 96 % avec les
tests originaux et les tests générés corrigés.

Le rapport final est conservé dans
`rapports/resultat-final/pit-reports/`.

Ce résultat signifie que tous les mutants générés lors de cette
analyse sont détectés ; il ne garantit pas l’absence de tout défaut.

## Commandes de reproduction

Les commandes Maven ci-dessous sont exécutées depuis `tika-core`.
Ollama doit être démarré pour la génération.

### Préparer le modèle local

```powershell
ollama pull qwen2.5-coder:1.5b
ollama cp qwen2.5-coder:1.5b codeqwen:v1.5-chat
```

### Générer les tests

```powershell
mvn "-Djava.version=22.0.0" io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1:class "-DselectClass=org.apache.tika.utils.DateUtils"
```

La propriété `java.version=22.0.0` contourne l’erreur de lecture du
format de version `22` rencontrée avec ChatUniTest. Elle ne change
pas le JDK utilisé.

### Exécuter tous les tests du module

```powershell
mvn test
```

### Analyser les tests originaux

```powershell
mvn test-compile org.pitest:pitest-maven:mutationCoverage "-DtargetClasses=org.apache.tika.utils.DateUtils" "-DtargetTests=org.apache.tika.metadata.TestMetadata"
```

### Analyser les tests originaux et les tests générés corrigés

```powershell
mvn test-compile org.pitest:pitest-maven:mutationCoverage "-DtargetClasses=org.apache.tika.utils.DateUtils" "-DtargetTests=org.apache.tika.metadata.TestMetadata,org.apache.tika.utils.DateUtils_tryToParse_6_2_Test"
```

### Analyser les trois groupes de tests

```powershell
mvn test-compile org.pitest:pitest-maven:mutationCoverage "-DtargetClasses=org.apache.tika.utils.DateUtils" "-DtargetTests=org.apache.tika.metadata.TestMetadata,org.apache.tika.utils.DateUtils_tryToParse_6_2_Test,org.apache.tika.utils.DateUtilsManualTest"
```

Chaque analyse produit ses rapports dans `tika-core/target/pit-reports`.
Les rapports doivent être conservés séparément avant de lancer
l’analyse suivante.

## Vérification de l’exécution

Après l’ajout des tests générés corrigés et du test manuel, la commande
`mvn test` a été exécutée dans le module `tika-core`. Tous les tests
réussissent et le contrôle Checkstyle passe également `BUILD SUCCESS`.


## Vérification avec GitHub Actions

Après l'intégration des tests générés et du test manuel, nous avons vérifié leur exécution dans GitHub Actions afin d'évaluer leur fonctionnement dans un environnement distinct de l'environnement local.

### Correction d'une incompatibilité avec Forbidden APIs

Lors de la première exécution du workflow `main jdk17 build`, la compilation Maven a échoué pendant la vérification `forbiddenapis:testCheck` du module `tika-core`.

Deux appels au constructeur `SimpleDateFormat(String)` ont été signalés dans `DateUtils_tryToParse_6_2_Test.java`, aux lignes 64 et 78 du fichier avant correction.

Ce constructeur utilise implicitement la locale par défaut du système, ce qui est interdit par les règles de vérification d'Apache Tika.

Nous avons corrigé les deux appels en précisant explicitement `Locale.ROOT` et en ajoutant l'import correspondant :

```java
import java.util.Locale;
```

```java
new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT)
```

La correction a été enregistrée dans le commit `0549c2a`. Lors de la nouvelle exécution, le build Linux Java 17 a dépassé la vérification qui avait initialement échoué et a poursuivi l'exécution des autres modules du projet.

### Vérifications sur Windows

Les workflows Windows Java 17 ont échoué pendant l'étape `actions/checkout`, avant la compilation et l'exécution des tests.

Les journaux indiquent l'erreur `Filename too long`, causée par certains chemins de fichiers particulièrement longs dans Apache Tika.

Cet échec ne permet donc pas de conclure à un défaut dans les tests de `DateUtils`, puisque ceux-ci n'ont pas été exécutés dans ces workflows.

### Autres vérifications

Le workflow `no split packages` a réussi.

Le workflow Docker snapshot a échoué, mais sa cause n'a pas encore été déterminée. Le succès du workflow `no split packages` ne constitue pas à lui seul une validation des tests unitaires.

Ces vérifications montrent l'importance de distinguer les erreurs du code testé des problèmes de configuration ou d'exécution du projet complet.

### Résultat du build Linux Java 17

Le workflow `main jdk17 build` a été exécuté sur la branche `main`, après la correction des deux appels à `SimpleDateFormat` dans le commit `0549c2a`.

Le build a réussi les vérifications du module `tika-core`, qui contient la classe `DateUtils` et nos tests supplémentaires.

L'exécution complète a toutefois échoué après environ 53 minutes, lors des tests d'intégration du module `tika-pipes-s3-integration-tests`, à l'étape Maven Surefire. Cet échec concerne un module distinct de celui étudié dans notre travail.

Les vérifications du module `tika-core` ont donc réussi dans cette exécution, mais le workflow global n'a pas obtenu le statut `BUILD SUCCESS`.

## Déclaration d'utilisation de l'IA générative

Nous avons utilisé ChatUniTest avec le modèle `qwen2.5-coder:1.5b` via Ollama pour générer des tests unitaires.

ChatGPT a également été utilisé par les deux membres de l'équipe comme outil d'assistance pour la compréhension du projet, la résolution de problèmes techniques, l'analyse des résultats et la rédaction de la documentation.

Les tests et les résultats présentés ont été examinés et validés par notre équipe.






# Documentation originale d’Apache Tika
Welcome to Apache Tika  <https://tika.apache.org/>
=================================================

[![license](https://img.shields.io/github/license/apache/tika.svg?maxAge=2592000)](http://www.apache.org/licenses/LICENSE-2.0)
[![Jenkins](https://img.shields.io/jenkins/s/https/ci-builds.apache.org/job/Tika/job/tika-main-jdk17.svg?maxAge=3600)](https://ci-builds.apache.org/job/Tika/job/tika-main-jdk17/)
[![Jenkins tests](https://img.shields.io/jenkins/t/https/ci-builds.apache.org/job/Tika/job/tika-main-jdk17.svg?maxAge=3600)](https://ci-builds.apache.org/job/Tika/job/tika-main-jdk17/lastBuild/testReport/)
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.tika/tika.svg?maxAge=86400)](http://search.maven.org/#search|ga|1|g%3A%22org.apache.tika%22)

Apache Tika(TM) is a toolkit for detecting and extracting metadata and structured text content from various documents using existing parser libraries.

Tika is a project of the [Apache Software Foundation](https://www.apache.org).

Apache Tika, Tika, Apache, the Apache feather logo, and the Apache Tika project logo are trademarks of The Apache Software Foundation.

Quick Start
===========

**Parse a file in Java:**

```java
import org.apache.tika.Tika;

Tika tika = new Tika();
String text = tika.parseToString(new File("document.pdf"));
System.out.println(text);
```

**From the command line:**

```bash
java -jar tika-app-*.jar --text document.pdf
```

**Maven dependency:**

```xml
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-parsers-standard-package</artifactId>
    <version>4.x.y</version>
    <type>pom</type>
</dependency>
```

Getting Started
===============
Pre-built binaries of Apache Tika standalone applications are available
from https://tika.apache.org/download.html . Pre-built binaries of all the
Tika jars can be fetched from Maven Central or your favourite Maven mirror.

**Tika 2.X and support for Java 8 reached End of Life (EOL) in April, 2025. 
See [Tika Roadmap 2.x, 3.x and beyond](https://cwiki.apache.org/confluence/display/TIKA/Tika+Roadmap+--+2.x%2C+3.x+and+Beyond).** 

Tika is based on **Java 17** and uses the [Maven 3](https://maven.apache.org) build system.
**N.B.** [Docker](https://www.docker.com/products/personal) is used for tests in tika-integration-tests. If Docker is not installed, those tests are skipped.

To build Tika from source, use the following command in the main directory:

    ./mvnw clean install

The Maven wrapper (`mvnw`) is included in the repository and will automatically download
the correct Maven version if needed. On Windows, use `mvnw.cmd` instead.

The build consists of a number of components, including a standalone runnable jar that you can use to try out Tika features. You can run it like this:

    java -jar tika-app/target/tika-app-*.jar --help


To build a specific project (for example, tika-server-standard):

    ./mvnw clean install -am -pl :tika-server-standard

If the ossindex-maven-plugin is causing the build to fail because a dependency
has now been discovered to have a vulnerability:

    ./mvnw clean install -Dossindex.skip


Faster Builds
=============

**Fast profile** - Use `-Pfast` to skip tests, checkstyle, and spotless:

    ./mvnw clean install -Pfast

**Parallel builds** - Add `-T1C` to build with 1 thread per CPU core:

    ./mvnw clean install -Pfast -T1C

**Maven Daemon (mvnd)** - Keeps a warm JVM running for 2-3x faster rebuilds:

```bash
# Install: https://github.com/apache/maven-mvnd
# macOS: brew install mvndaemon/tap/mvnd

# Use exactly like mvn
mvnd clean install -Pfast
mvnd test -pl :tika-core
```

**Combine both** for maximum speed during development:

    mvnd clean install -Pfast -T1C


Reproducible Builds
===================

Apache Tika supports [reproducible builds](https://reproducible-builds.org/). This means
that building the same source code with the same JDK version should produce
byte-for-byte identical artifacts, regardless of the build machine or time.

Key configuration:
- `project.build.outputTimestamp` is set in `tika-parent/pom.xml`
- All Maven plugins are configured to produce deterministic output

To verify the build plan supports reproducibility:

    ./mvnw artifact:check-buildplan

To verify two builds produce identical artifacts:

    ./mvnw clean install -DskipTests
    mv ~/.m2/repository/org/apache/tika tika-build-1
    ./mvnw clean install -DskipTests
    diff -r tika-build-1 ~/.m2/repository/org/apache/tika


Maven Dependencies
==================

Apache Tika provides *Bill of Material* (BOM) artifact to align Tika module versions and simplify version management. 
To avoid convergence errors in your own project, import this
bom or Tika's parent pom.xml in your dependency management section.

If you use Apache Maven:

```xml
<project>
  <dependencyManagement>
    <dependencies>
      <dependency>
       <groupId>org.apache.tika</groupId>
       <artifactId>tika-bom</artifactId>
       <version>4.x.y</version>
       <type>pom</type>
       <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <dependency>
      <groupId>org.apache.tika</groupId>
      <artifactId>tika-parsers-standard-package</artifactId>
      <type>pom</type>
      <!-- version not required since BOM included -->
    </dependency>
  </dependencies>
</project>
```

For Gradle:

```kotlin
dependencies {
  implementation(platform("org.apache.tika:tika-bom:4.x.y"))

  // version not required since bom (platform in Gradle terms)
  implementation("org.apache.tika:tika-parsers-standard-package@pom")
}
```

Migrating to 4.x
================
TBD

Contributing
============
See [CONTRIBUTING.md](CONTRIBUTING.md) and https://tika.apache.org/contribute.html

[![contributors](https://contributors-img.web.app/image?repo=apache/tika)](https://github.com/apache/tika/graphs/contributors)

Building from a Specific Tag
============================
Let's assume that you want to build the 3.0.1 tag:
```
0. Download and install hub.github.com
1. git clone https://github.com/apache/tika.git
2. cd tika
3. git checkout 3.0.1
4. ./mvnw clean install
```

If a new vulnerability has been discovered between the date of the
tag and the date you are building the tag, you may need to build with:

```
4. ./mvnw clean install -Dossindex.skip
```

If a local test is not working in your environment, please notify
 the project at dev@tika.apache.org. As an immediate workaround,
 you can turn off individual tests with e.g.:

```
4. ./mvnw clean install -Dossindex.skip -Dtest=\!UnpackerResourceTest#testPDFImages
```

License (see also LICENSE.txt)
==============================

Collective work: Copyright 2011 The Apache Software Foundation.

Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at

<https://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the specific language governing permissions and limitations under the License.

Apache Tika includes a number of subcomponents with separate copyright notices and license terms. Your use of these subcomponents is subject to the terms and conditions of the licenses listed in the LICENSE.txt file.

Export Control
==============

This distribution includes cryptographic software.  The country in which you currently reside may have restrictions on the import, possession, use, and/or re-export to another country, of encryption software.  BEFORE using any encryption software, please  check your country's laws, regulations and policies concerning the import, possession, or use, and re-export of encryption software, to  see if this is permitted.  See <http://www.wassenaar.org/> for more information.

The U.S. Government Department of Commerce, Bureau of Industry and Security (BIS), has classified this software as Export Commodity Control Number (ECCN) 5D002.C.1, which includes information security software using or performing cryptographic functions with asymmetric algorithms.  The form and manner of this Apache Software Foundation distribution makes it eligible for export under the License Exception ENC Technology Software Unrestricted (TSU) exception (see the BIS Export Administration Regulations, Section 740.13) for both object code and source code.

The following provides more details on the included cryptographic software:

Apache Tika uses the Bouncy Castle generic encryption libraries for extracting text content and metadata from encrypted PDF files.  See <http://www.bouncycastle.org/> for more details on Bouncy Castle.  

Mailing Lists
=============

* user@tika.apache.org - About using Tika
* dev@tika.apache.org - About developing Tika

Subscribe by sending a message to `{list}-subscribe@tika.apache.org`.

Issue Tracker
=============

https://issues.apache.org/jira/browse/TIKA

Security
========

See [SECURITY.md](SECURITY.md) and https://tika.apache.org/security.html
