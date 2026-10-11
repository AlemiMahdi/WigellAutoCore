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