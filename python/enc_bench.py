import datetime
import random

from pymongo import MongoClient
from pymongo.collection import Collection

import rand_docs
import queryable_encryption_helpers as helpers
from dotenv import load_dotenv


def now():
    return datetime.datetime.now()


class BenchClient:
    coll: Collection
    hits: list[str]
    name: str

    def __init__(self, uri: str, metadata: dict):
        self.uri = uri
        self.conn_params = {}
        self.metadata = metadata
        self.dbname = metadata.get('db')
        self.collname = metadata.get('coll')

    def init_client(self):
        self.mongo_client = MongoClient(self.uri, **self.conn_params)
        self.coll = self.mongo_client[self.dbname][self.collname]
        self.coll.drop()

    def insert(self, num_docs: int):
        start_ts = now()
        self.hits = rand_docs.create(num_docs, self.coll)
        duration = now() - start_ts
        print(f'{self.name} inserted {num_docs} in {duration}')

    def find(self):
        misses = rand_docs.get_misses(self.hits)
        finds = self.hits + misses
        random.shuffle(finds)
        found = 0
        missed = 0
        start_ts = now()
        for f in finds:
            find = self.coll.find_one({'patientRecord.ssn': f})
            if find is not None:
                found += 1
            else:
                missed += 1
        duration = now() - start_ts
        print(f'{self.name} found {found} and missed {missed} in {duration}')

    def delete(self):
        start_ts = now()
        for h in self.hits:
            self.coll.delete_one({'patient.ssn': h})
        duration = now() - start_ts
        print(f'{self.name} deleted {len(self.hits)} docs in {duration}')

    def close(self):
        self.mongo_client.close()


class PlainClient(BenchClient):
    def __init__(self, uri: str, metadata: dict):
        super().__init__(uri, metadata)
        self.name = 'PlainClient'

    def insert(self, num_docs: int):
        super().insert(num_docs)
        self.coll.create_index('patientRecord.ssn')


class QEClient(BenchClient):
    def __init__(self, uri: str, metadata: dict):
        super().__init__(uri, metadata)
        self.kms_provider = self.metadata['kms_provider']
        self.keyvault_db_name = self.metadata['keyvault_db_name']
        self.keyvault_coll_name = self.metadata['keyvault_coll_name']
        self.kv_ns = f'{self.keyvault_db_name}.{self.keyvault_coll_name}'
        self.kms_provider_credentials = helpers.get_kms_provider_credentials(self.kms_provider)
        self.auto_enc_options = helpers.get_auto_encryption_options(self.kms_provider, self.kv_ns,
                                                                    self.kms_provider_credentials)
        self.conn_params['auto_encryption_opts'] = self.auto_enc_options
        self.name = 'QEClient'

    def init_client(self):
        super().init_client()
        self.mongo_client[self.keyvault_db_name][self.keyvault_coll_name].drop()
        self.encrypted_fields_map = self.metadata['encrypted_fields_map']
        self.client_encryption = helpers.get_client_encryption(self.mongo_client, self.kms_provider,
                                                               self.kms_provider_credentials, self.kv_ns)
        self.customer_master_key_credentials = helpers.get_customer_master_key_credentials(self.kms_provider)
        self.client_encryption.create_encrypted_collection(
            self.mongo_client[self.dbname],
            self.collname,
            self.encrypted_fields_map,
            self.kms_provider,
            self.customer_master_key_credentials
        )


def run_it():
    load_dotenv()
    uri = 'mongodb://localhost:27017,localhost:27018,localhost:27019'
    num_docs = 10_000

    # run_plain_client(uri, num_docs)

    # run_qe1(num_docs, uri)

    run_qe2(num_docs, uri)


def run_qe1(num_docs, uri):
    meta = {
        'db': 'medicalRecords',
        'coll': 'patients',
        'keyvault_db_name': 'encryption',
        'keyvault_coll_name': '__keyVault',
        'kms_provider': 'local',
        'encrypted_fields_map': {
            'fields': [
                {
                    'path': 'patientRecord.ssn',
                    'bsonType': 'string',
                    'queries': [{'queryType': 'equality'}]
                },
                {
                    'path': 'patientRecord.billing',
                    'bsonType': 'object'
                }
            ]
        }
    }
    qe_client = QEClient(uri, meta)
    qe_client.init_client()
    qe_client.insert(num_docs)
    qe_client.find()
    qe_client.delete()


def run_qe2(num_docs, uri):
    meta = {
        'db': 'medicalRecords',
        'coll': 'patients',
        'keyvault_db_name': 'encryption',
        'keyvault_coll_name': '__keyVault',
        'kms_provider': 'local',
        'encrypted_fields_map': {
            'fields': [
                {
                    'path': 'patientRecord.ssn',
                    'bsonType': 'string',
                    'queries': [{'queryType': 'equality', 'contention': 64}]
                },
                {
                    'path': 'patientName',
                    'bsonType': 'string',
                    'queries': [{'queryType': 'equality', 'contention': 64}]
                },
                {
                    'path': 'patientRecord.billing.number',
                    'bsonType': 'string',
                    'queries': [{'queryType': 'equality', 'contention': 64}]
                }
            ]
        }
    }
    qe_client = QEClient(uri, meta)
    qe_client.init_client()
    qe_client.insert(num_docs)
    qe_client.find()


def run_plain_client(uri, num_docs):
    meta = {
        'db': 'medicalRecords',
        'coll': 'patients'
    }
    plain_client = PlainClient(uri, meta)
    plain_client.init_client()
    plain_client.insert(num_docs)
    plain_client.find()
    plain_client.delete()


if __name__ == '__main__':
    run_it()
