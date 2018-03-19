Java serialization tool of high flexibility and good performance.

Purpose: provides alternative extensible solution for objects serialization to/from JSON/BJSON.

Simplest usage: 
  ssg.serialize.impl.JSON static methods: 
    standard: toJSON / toPrettyJSON / toBJSON / fromJSON / from BJSON
    extended: toJSONX / toPrettyJSONX / toBJSONX / fromJSONX / from BJSONX

Usage:
  instantiate ssg.serialize.impl classes and use to/from method variants:
    JSONSerializer
    BJSONSerializer
    BASE64Serializer
    BASE64InputStream
    BASE64OutputStream
    UTF8Serializer

Special:
   ssg.serialize.utils.DeepCompare static methods:
     diff - compares 2 object's structure/values and returns difference description

Notice:
   see Functional* tests for comparison with other Java implementations and java built-in serialization
   see DESCRIPTION.txt for ideology...

Features: allows serialization of complex objects with cyclic references by introducing reference values
(as string "${REF:<num>}" in JSON and special type (111) in BJSON). Provides alternative 
Base64 and UTF-8 encoding/decoding.

Additions: provides tool for deep loosy comparison of structures (e.g. can check if original object and 
its serialized variant(s) match). Uses reflection-based alternative comparable serializers prefrormance 
tests (see Functional* tests).

TODO: add other JSON-like formats, XML, ASN1 (BER, DER).
