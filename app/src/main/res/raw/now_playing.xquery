(: Must be formatted with String.format for use. Argument 1 is the current time :)
declare default element namespace "http://www.worlddab.org/schemas/spi/31";
(for $prog in //programme
let $progTime := xs:dateTime($prog/location/time/@time)
where $progTime lt xs:dateTime(%1$s)
order by $progTime descending
return $prog/longName/text())
[position() = 1]