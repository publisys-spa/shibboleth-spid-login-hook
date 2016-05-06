# Shibboleth SPID Auto Login Hook

Liferay Hook  per integrare un portale Liferay con un **Service Provider Shibboleth**.

> La versione di riferimento di **Liferay** supportata è la **6.0.6**.

### Configurazione preliminare Liferay
È necessario definire, tramite Pannello di controllo, dei campi personalizzati sull'entità utente di Liferay per consentire la memorizzazione delle proprietà che transitano tramite Shibboleth. I nomi dei campi personalizzati sono:
* **fiscalcode**
* **pec**
* **birthplace**
* **birthplace-stato**
* **telephone**
* **mobile-phone**
* **validate**

Ogni campo personalizzato dovrà essere di tipo **TESTO** e avere i privilegi sull'**Owner** (Elimina, Permessi, Aggiorna, Visualizza) e sullo **User** (Aggiorna, Visualizza).

### Generazione WAR dell'HOOK
Nel POM (Project Object Model) Maven, creare un profile nel quale bisogna indicare le seguenti properties:
* **shibboleth.enabled** (*true/false per abilitare/disabilitare l'HOOK che effettua l'AutoLogin tramite parametri ricevuti da  Shibboleth*)
* **shibboleth.logout.url** (*indicare l'URL di Logout di Shibboleth*)
* **logout.redirect.url** (*indicare l'URL alla quale si vuole reindirizzare l'utente dopo la Logout*)

```sh
<profile>
    <id>dev</id>
    <properties>
        <shibboleth.enabled>true</shibboleth.enabled>
        <shibboleth.logout.url>http://dev.publisys.it/Shibboleth.sso/Logout</shibboleth.logout.url>
        <logout.redirect.url>http://www.publisys.it</logout.redirect.url>
    </properties>
</profile>
```
Effettuata la precedente configurazione è possibile eseguire il comando
```sh
$ mvn clean package -Pdev
```
dove **dev** è l'**ID** del profilo creato nel pom.xml.

Eseguito il comando copiare il WAR generato dalla directory target nella directory di deploy di Liferay per l'installazione e riavviare Liferay.

[Publisys S.p.A.][publisys]

[publisys]: <http://www.publisys.it>
