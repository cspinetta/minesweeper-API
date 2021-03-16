#!/usr/bin/env python3

import requests as requests

hostname = 'localhost:9000'


def run():
    print("Starting the game...")
    api_client = MinesweeperAPIClient("cspinetta", "123456789")
    result = api_client.new_game(10, 10, 10)
    print(result)


class MinesweeperAPIClient:

    def __init__(self, user, password):
        self.user = user
        self.password = password
        self.session = requests.Session()
        self.session.auth = (user, password)

    def new_game(self, height, width, mines):
        r = self.session.post(f'http://{hostname}/games', json={'height': height, 'width': width, 'mines': mines})
        if r.status_code >= 300:
            exit(f"somethign went wrong!\nStatus code: {r.status_code}\nReason: {r.text}")
        return r.json()

    def reveal(self, game_id, x, y):
        return self.actionOnACell(game_id, x, y, 'reveal')

    def addRedFlag(self, game_id, x, y):
        return self.actionOnACell(game_id, x, y, 'set-red-flag')

    def addQuestionFlag(self, game_id, x, y):
        return self.actionOnACell(game_id, x, y, 'set-red-flag')

    def cleanFlag(self, x, y):
        return self.actionOnACell(x, y, 'set-red-flag')

    def actionOnACell(self, game_id, x, y, action):
        r = self.session.patch(f'http://{hostname}/games/{game_id}',
                               json={'action': action, 'position': {'x': x, 'y': y}})
        if r.status_code >= 300:
            exit(f"somethign went wrong!\nStatus code: {r.status_code}\nReason: {r.text}")
        return r.json()

    def draw(self, game_id, debug=False):
        r = self.session.get(f'http://{hostname}/games{game_id}/ascii', params={'debug': debug})
        if r.status_code >= 300:
            exit(f"somethign went wrong!\nStatus code: {r.status_code}\nReason: {r.text}")
        return r.json()


if __name__ == "__main__":
    run()
