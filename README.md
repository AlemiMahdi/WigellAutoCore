Har lagt till ENUMs för arbetsorder typer och nytt Package workOrderType.
Några nya klasser, byggt med kombination av designmönster factory method och strategy
Lagt till en attributeConverter, för att gammal data inte ska göra att de krashar.

Finns en klass av varje Planned, drop-in, complaint som implementerar interface och man kan sen bygga vidare och anpassa metoderna utifrån vad varje arbetstyp behöver.

När man klickar Starta arbetsorder så kommer det nu upp som alternativ där att välja Planerad som standard värde eller välja Drop-in, reklamation i rulllistan.

WAC-60 och WAC-61 kan bygga vidare på det

Jag tar bort i Main:
i metoden createWorkOrder
//        garageSystem.createWorkOrder(
//                bookingId,
//                mechanicId, WorkOrderTypeEnum.PLANNED
//        );

För att det används inte längre och onödigt att bygga om det för 
att konsolappen ska fungera, viktigast är gränssnittet just nu



---

## Main 2 – Fristående program (WAC-106)

`com.wac.autocore.standalone.StandaloneMain` kör två verksamhetsoperationer utan Spring, JavaFX och databas:
- Statusbyte på arbetsorder (samma regel som appen, `WorkOrderStatus`)
- Krockkontroll för bokningar (samma regel som appen, `BookingOverlapChecker`)

Programmet använder samma användare och roller som appen, så det behöver samma miljövariabler.

### 1. Skapa `.env` i projektroten
Filen finns i `.gitignore` och ska **aldrig** committas.

```
WAC_ADMIN_PASSWORD=välj-ett-lösenord
WAC_WORKSHOP_PASSWORD=välj-ett-lösenord
WAC_VIEWER_PASSWORD=välj-ett-lösenord
```

### 2. Run Configuration i IntelliJ
1. **Run → Edit Configurations → + → Application**
2. **Main class:** `com.wac.autocore.standalone.StandaloneMain`
3. **Environment variables:** klicka på mappikonen och välj `.env`
4. Kör från rullistan uppe till höger (inte från pilen i koden, då följer inte `.env` med)

### 3. Logga in i konsolen
| Användarnamn | Lösenord | Resultat |
|---|---|---|
| `admin` | `WAC_ADMIN_PASSWORD` | Operationerna körs |
| `mechanic` | `WAC_WORKSHOP_PASSWORD` | Operationerna körs |
| `viewer` | `WAC_VIEWER_PASSWORD` | `Access denied` – nekas båda |
| fel lösenord | – | `Login failed` – inget körs |



