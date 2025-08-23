package com.sterle;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class EntityMsg {
    // Root message record
record EntityMessage(
        @JacksonXmlProperty(localName = "MessageHeader")
        MessageHeader messageHeader,

        @JacksonXmlProperty(localName = "ObjectState")
        String objectState,

        @JacksonXmlProperty(localName = "MessageData")
        MessageData messageData
) {}

// Header part
record MessageHeader(
        @JacksonXmlProperty(localName = "ServiceId")
        IdName serviceId,

        @JacksonXmlProperty(localName = "SystemId")
        IdName systemId
) {}

// UUID + Name holder
record IdName(
        @JacksonXmlProperty(localName = "UUID")
        String uuid,

        @JacksonXmlProperty(localName = "Name")
        String name
) {}

// Data wrapper
record MessageData(
        @JacksonXmlProperty(localName = "Entity")
        Entity entity
) {}

// The entity itself
record Entity(
        @JacksonXmlProperty(localName = "UUID")
        String uuid,

        @JacksonXmlProperty(localName = "Lat")
        double lat,

        @JacksonXmlProperty(localName = "Lon")
        double lon
) {}
}
