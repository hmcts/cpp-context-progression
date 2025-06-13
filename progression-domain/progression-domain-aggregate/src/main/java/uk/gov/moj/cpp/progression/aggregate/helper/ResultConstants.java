package uk.gov.moj.cpp.progression.aggregate.helper;

import static java.util.UUID.fromString;

import java.util.UUID;

@SuppressWarnings({"squid:S1118"})
public class ResultConstants {

    // Appeal Results
    // Conviction, Sentence and Conviction and Sentence Shared
    public static final UUID APA = fromString("48b8ff83-2d5d-4891-bab1-b0f5edcd3822");//Appeal Abandoned
    public static final UUID AW = fromString("453539d1-c1a0-475d-9a02-16a659e6bc34");//Appeal Withdrawn

    // Conviction
    public static final UUID AACD = fromString("548bf6b7-d152-4f08-8c9e-a079bf377e9b");//Appeal against Conviction Dismissed
    public static final UUID ASV = fromString("a2640f68-104b-4ef6-9758-56956bd61825");// Appeal against Conviction Dismissed & Sentence Varied

    // Sentence
    public static final UUID AASD = fromString("573b195d-5795-43f1-92bb-204de1305b8b");//Appeal against Sentence Dismissed
    public static final UUID SV = fromString("55f15ecf-ea80-40f4-848d-29e7b8d73ae2");//sentence varied
    public static final UUID AASA = fromString("d861586a-df88-440d-98d4-63cc2a680ae1");//Appeal against Sentence Allowed

    // Conviction and Sentence
    public static final UUID ACSD = fromString("3b1f0a20-15cf-4795-98b1-ea87ebab2ec6");//Appeal against Conviction and Sentence Dismissed

    public static final UUID AACA = fromString("177f29cd-49e9-41d1-aafb-5730d7414dd4");//Conviction Allowed

    // Statutory and Reopen Shared
    public static final UUID RFSD = fromString("d3902789-4cc8-4753-a15f-7e26dd39f6ae");//Application refused
    public static final UUID G = fromString("2b3f7c20-8fc1-4fad-9076-df196c24b27e");//Granted
    public static final UUID WDRN = fromString("eb2e4c4f-b738-4a4d-9cce-0572cecb7cb8");//Withdrawn

    // Statutory Declaration
    public static final UUID STDEC = fromString("e2f6e11b-c3a2-4e76-8d29-fbede4174988");//Statutory Declaration
    public static final UUID DISM = fromString("14d66587-8fbe-424f-a369-b1144f1684e3");//Dismissed

    // Reopen Case
    public static final UUID ROPENED = fromString("e3fb46ee-e406-4f73-9bf1-71d513da8cc7");//Case reopened

    // Breach Results

    // Breach Result IDs
    public static final UUID OREV = fromString("2e83102c-a262-4bb3-a48d-c2107a094a66");//Order revoked
    public static final UUID BRO = fromString("1c70f1e0-7674-4acb-8710-1c5233e984c0");//No adjudication - dealt with for original offence
    public static final UUID OTC = fromString("14f22747-685e-4bd9-91e7-fb372d8c3b72");//Order to continue

    // Confiscation Order Results
    public static final UUID CONFAA = fromString("76b02133-4927-4f21-9f79-1ce361a17b0f");//Confiscation order where Court decides the available amount - makes a statement of findings
}
