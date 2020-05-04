# Changelog App OBC

Tutte le modifiche rilevanti a questo progetto saranno documentate in questo file.

## [0.112.5p] - 2020-04-02
### VersionCode=196
### Changes
- new release for MySharengo project (#950)

## [0.112.5p] - 2020-03-11
### VersionCode=196
### Changes
- removed close trip button for Italian realease

## [0.112.5] - 2020-03-09
### VersionCode=195
### Changes
- removed SOS management for Italian realease

## [0.112.4] - 2019-12-09
### VersionCode=194
### Changes
- changed translations (Slovenia)

## [0.112.3] - 2019-11-22
### VersionCode=193
### Changes
- added internationalized strings to GoodBye forms (f_goodbye.xml)

## [0.112.2] - 2019-10-24
### VersionCode=192
### Changes
- implemented navigator function using localized maps based on the release
- implemented navigator function using localized voices based on the release

## [0.112.1] - 2019-09-25
### VersionCode=191
### Changes
- update app with new release (Slovenia)
- added use of local banners for non-Italian releases

## [0.112.0] - 2019-07-08
### VersionCode=190
### Changes
- aggiunte chiavi e password pubblicazione APK
- commentato startRemotePoiCheckCycle() e stopRemotePoiCheckCycle()
- incrementato time-out connessione http e https da 40 a 80 sec
- correzione messaggi di warning (scope delle variabili, metodi e variabili inutilizzati, etc.)
- commentato codice inutilizzato
- normalizzazione dei messaggi di errore e di debug
- inserito time stamp nei messaggi di log

### Cancellazioni
- rimosse le classi FDamegesNew.java e FDrunk.java in quanto non utilizzate


## [0.111.0] - 2019-05-21
### VersionCode=189
### Modifiche (ultima di Fulvio)
- rimozione procedura spegnimento OBC
- correzione bug chiusura corsa da remoto

## 0.110
- procedura spegnimento OBC

## 0.109.x                                11/10/2018
- Reboot automatico in caso di 350A
- invio corse offline ogni 5 min (se presenti)
- scarta SOS vecchi più di 10 min (60 in precedenza)
- ripristinati KM nel Beacon
- Introdotta chiusura remota
- reintrodotti poi su navigatore
- predisposta app per porting verso altri paesi (slovakia)
- fix in pagina di chiusura (non chiudeva correttamente la pagina)
- fix per traduzioni mancanti

## 0.108.19                            11/10/2018
- fix procedura di ricarica non chiede più pin agli utenti non manutentori

## 0.108.18                            17/01/2018
- fix chiusura corsa non resta più bloccato

## 0.108.17                            17/01/2018
- fix parent_id, non invia più -2 

## 0.108.16                            17/01/2018
- fix per eventi annulla fine corsa
- fix per aperture multiple
- fix per cora non si chiude
- aggiunti log per lifecycle delle pagine (per vedere le azioni dell’utente)
- Priorizzati SOS nell’invio degli eventi offline
- diminuito a 30Sec delay fra invio eventi

## 0.108.15                            17/01/2018
- fix per plug che non si aggiorna

## 0.108.14                            17/01/2018
- bug fix per charging e plug
- aggiunta chiusura remota
- fix per file aperti (problema apertura doppia)

## 0.108.3                             17/01/2018
- bug fix
- Implementazione nuove APi in NodeJs

## 0.108.0                             17/01/2018
- Unplug: funzione di stacco della carica da parte dell’utente (per ora disattiva)
- Odometro virtuale: calcolo dei kilometri percorsi dal veicolo
- data logger: analizzatore dati per il nuovo SOC virtuale
- Fix problema apertura corsa utente neo iscritto
- Miglioramento API di comunicazione con il server
- Fix problema di malfunzionamento DNS delle SIM (vedi problema Vodafone)
- Tempo di tenuta modalità aereo da 5 sec a 15 sec
- Visualizzazione dell’area di servizio sul navigatore
- Distinzione notifiche SOS o Danni
- Popup allarmi SOC anche nella home page

## 0.107.4                             17/01/2018
- BugFix

## 0.107.2                             17/01/2018
- Modificate icone in home
- Aggiunte icone per nuove funzionalità
- centrato video della Home
- controllo chiave su off per chiudere corsa o sosta

## 0.107.1                             17/01/2018
- Rimosso bottone musica
- fix bottone help nella mappa
- fix bug in visualizzazione documenti
- inserito noGps alarm (per mandare O_O_O in futuro)

## 0.107                               17/01/2018
- modificata la home in Menu
- aggiunto video alla home
- aggiunto libretto e assicurazione alla home
- tolto bottone termina corsa dalla mappa e inseriti bottoni separati nella home

## 0.106.8                               30/10/2017
- cambiate tempistiche di comunicazione col server

## 0.106.7                               10/10/2017
- fix invii multipli comunicazione apertura B2B
- fix check pin se user B2B disabilitato

## 0.106.6                               10/10/2017
- fix restart ZMQ
- non resetta contatore 3 ore

## 0.106.5                               10/10/2017
- fix gestione log
- invio npin all’inserimento (B2B)

## 0.106.3                               10/10/2017
- fix dimensione log
- vari bugfix per stabilità
- introdotte tessere di sola apertura
- introdotto battery safety
- inserimento logica B2B

## 0.105.7.4                               26/07/2017
- fix primo pin errato
- modifiche calcolo soc, esclusione errore amp

## 0.105.7                                  11/07/2017
- NON è presente lo spegnimento automatico
- Nuovo controllo per evitare contaminazione dei dati nell’invio delle corse.
- Ridotta la quantità di dati inviati al server
- Introdotta nuova funzione nella Home, si può ascoltare la musica non il cavo aux
- risolto problema dove a volte non veniva ripristinato lo stato dell’audio dopo un avviso vocale
- Introdotto nuovo meccanismo di controllo dell’ora
### INDICAZIONI PER TEST
- provare che con apertura di corse ravvicinate non ci siano problemi su admin.
- provare con apertura multipla di corse un caso di auto offline risultno poi su admin.
- controllare che la connessione col server resti stabile e che la macchina non vada O_O_O
- controllare che non ci siano problemi di orario sballato

## 0.105.6tris                                  --/--/2017
- Nuovo meccanismo per restore view in caso di corsa bloccata su 1sec
- RIMOSSO spegnimento automatico sotto il 10% dopo 3 ore se non in corsa non in carica e non 0% 

## 0.105.7                                  29/05/2017
- socR ignora il socBMS se a 0 ma valore celle disponibile, ignoro out amp se 350.
- introdotto spegnimento automatico sotto il 10% dopo 3 ore se non in corsa non in carica e non 0% 
- Introdotto evento per spegnimento sotto soglia di batteria
- Introdotto evento per segnalare anomalia CAN BUS

## 0.105.6                                  25/05/2017
- introdotto controllo fuso/utilizzo ora gps in caso di ora sballata.
- ridotto utilizzo mod aereo in caso di errore di connessione.

## 0.105.5                                  --/05/2017
- modifica calcolo socR ora considera il maggiore dei 2 valori bms se solo 1 va a 0

## 0.105.4                                  --/05/2017
- fix messaggio vocale

## 0.105.3                                  26/04/2017
- fix batteria bloccata
- nuovo vmax 83 DFD/ 82 HNLD

## 0.105.2                                  26/04/2017
- max invio offline eventi 1 settimana

## 0.105.1                                  19/04/2017
- modifica messaggi vocali multilingua (voci vecchie)
- aumentato tempo chiusura corsa senza pin a 5 minuti
- forzo chiusura corsa fine countdown chiusura

## 0.105                                  10/04/2017
- Implementazione bonus con parcheggio vicino a colonnina di ricarica
- nuovo calcolo range 
- timer di 10 sec prima di considerare l’auto rientrata nell’area operativa
- Modifiche soc virtuale tolta limitazione 5 in salita consumo A < 25 per considerare SOC2
- modifica gestione stato di pulizia auto
- inserito reboot giornaliero
- inserito meccanismo per reset connessione in caso di errore
- ottimizzata gestione dei file
- introduzione di Firenze nel Bonus
- incrementati log pagina sos
- Miglioramenti nella gestione della memoria
- correzioni varie per stabilità
- risolto bug di apertura errata di pagine dopo crash
- gestione restart protocollo per notifica comandi server
- miglioramento della gestione di corsa bloccata a 1sec 
- risoluzione problema consumo anomalo cpu
- passaggio automatico dalla prima schermata a quella successiva dopo 1 min
- nuovo controllo per fine corsa se dentro/fuori area operativa
- evento se utente entra/esce da area operativa
- fix avviso vocale fuori area operativa in home
- cambio firma, usare job CLASSIC
- Dopo un avviso vocale ricorda il volume precedente
- Evento per indicare il click di sosta o fine corsa
- Gestione multilingua per avvisi vocali
- nuovo banner offline
- rimosso il lampeggio del led rfid in sosta
- ridotto tempo chiusura corsa senza pin a 3 minuti
- rimossa bandiera francese e cinese dalla scelta della lingua

## 0.104.10                              13/02/2017
- Risolto problema apertura corsa via app
- modifica salvataggio dati corsa
- risolto bug dopo lettura di valore null nel virtual BMS
- correzioni varie per stabilità

## 0.104.9-bis                                30/01/2017
- alleggerimento del carico del processore per evitare crash dell’app

## 0.104.9-test                               25/01/2017
- chiusura messaggio soc dopo 20 secondi
- Modificato messaggio della mail di batteria scarica, aggiunto link
- aggiunta “X” per chiudere alert
- aggiunto controllo per evitare la chiusura della corsa se non richiesto
- ingrandito messaggio
- avviso sonoro a fine corsa per ricordare di spengere il quadro
- impostato navigatore per evitare autostrade

## 0.104.8-bms                              12/01/2017
### UTILIZZA SOCR
- Modifiche per stabilità 
- potenziati i log per seguire le azioni dell’utente
- modifiche per tempo di visualizzazione dei POI
- messaggio vocale + popup soc basso
- risolto problema radio attiva dopo fine corsa.


## 0.104.6-bms                              5/12/2016
### UTILIZZA SOCR
- Modificato calcolo V100% 

## 0.104.5-bms                              2/12/2016
### UTILIZZA SOCR
- introduzione del SOC_GPRS nella selezione del socBMS
- introdotto socAdmin e soc_GPRS nella schermata di servizio
- Dopo 10 secondi dalla fine del timer del banner iniziale la schermata si chiude da sola
- addolcimento delle variazioni di SOCR, se dislivello superiore a 5 modifico solo di 5
- Risolto problema rilevamento ingresso/uscita area operativa
- modificato popup marketing
- modificato messaggio di report crash

## 0.104.4-sk9                              1/12/2016
### NON UTILIZZA SOCR
- modifiche alla visualizzazione dei poi Evento, auto chiusura popup POI
- introdotto auto switch fra coordinate int/ext in caso di:coord 0.0; coord fisse per 3 campionamenti; coord fuori Italia. (tempo di campionamento: 2minuti)
- introduzione del SOC_GPRS nella selezione del socBMS
- introdotto socAdmin e soc_GPRS nella schermata di servizio

## 0.104.4-bms                              30/11/2016
- modifiche alla visualizzazione dei poi Evento, auto chiusura popup POI
- introdotto auto switch fra coordinate int/ext in caso di:coord 0.0; coord fisse per 3 campionamenti; coord fuori Italia. (tempo di campionamento: 2minuti)

## 0.104.3                              25/11/2016
- risolto problema schermata fissa radio/fine corsa
- nuovo sistema selezione soc2 se vbatt<67V o Vcella sotto soglia degradamento

## 0.104.2                              24/11/2016
- correzioni varie per stabilità
- introdotto bms virtuale
- gestione POI GIFT 
- nuovo avviso vocale per segnalare uscita area operativa

## 0.104.1
- gestione poi evento di natale.
- correzioni varie per stabilità

## 0.104
- risoluzione probelmi di visualizzazione bandiere per lingua al passaggio della card

## 0.103.5-sk9
- visualizzazione del socr nella schermata di servizio(utiizzo sempre il soc bms però!)
- risoluzione problemi di termina corsa dopo un crash

## SOCR TEST (0.103.5-bms)
- implementato BMS virtuale per gestire il SOC lato OBC leggendo dati dal CAN

## 0.103.4
- Corretta percentuale batteria per invio mail.
- Risolto problema prenotazioni multiple.
- Aggiunto cursore ai campi di ricerca navigatore
- Aggiunta compatibilità in assenza di mappe
- Aggiunta icona app

## 0.103.3
- Introdotto nuovo navigatore skobbler
- schermata rossa in manutenzione o O_O_O
- icona lampeggiante per fuori area o no 3G
- aggiunti bottoni vuoti in home
- aggiunto bottone per visualizzare FAQ nella schermata del PIM
- aggiunto avviso sonoro quando si esce dall’area operativa
- banner inizio e fine corsa
- aggiunta targa in schermata SOS
- aggiunta schermata di servizio dalla apgina sos (punto esclamativo)
- banner offline
- nuova tastiera per schermata cerca
- POI visualizzabili e cliccabili sulla mappa
- modificate icone 
- invio mail al di sotto del 10% soc (se in corsa)
