#!/usr/bin/env python3
import argparse
import logging

import requests as requests

hostname_dev = 'http://localhost:9000'
hostname_prod = 'https://cspinetta-minesweeper-api.herokuapp.com'
api_hostname = hostname_prod

user_action_new = 'new'
user_action_details = 'details'
user_action_delete = 'delete'
game_action_reveal = 'reveal'
game_action_red_flag = 'add-red-flag'
game_action_question_flag = 'add-question-flag'
game_action_clean = 'clean'


def run():
    try:
        parser = create_parser()
        args = parser.parse_args()

        api_client = MinesweeperAPIClient(args.user, args.password)

        if args.game_command == 'user':
            subcommand_user(api_client, args)
        elif args.play_actions == 'new':
            subcommand_game_new(api_client, args)
        elif args.play_actions == 'play':
            subcommand_game_play(api_client, args)
        else:
            subcommand_game_draw(api_client, args)
    except KeyboardInterrupt:
        pass
    except ApiUnexpectedResponseError as exp:
        logging.error(exp.message)
    except Exception:
        logging.exception("unexpected exception")


def create_parser():
    parser = argparse.ArgumentParser(description='Minesweeper game')

    game_subparsers = parser.add_subparsers(dest='game_command', required=True, help='game commands help')

    # sub-command user
    # e.g. user -u user -p pass -a new
    parser_player = game_subparsers.add_parser('user', help='User management')
    parser_player.add_argument('--action', '-a', nargs='?', dest='user_action',
                               choices=[user_action_new, user_action_details, user_action_delete],
                               default=user_action_details,
                               help='Action. Default: details')
    add_credentials_args(parser_player)

    parser_game = game_subparsers.add_parser('game', help='Play a game')

    parser_game_subcommands = parser_game.add_subparsers(dest='play_actions', required=True, help='play actions help')

    # sub-command game new
    # e.g. game new -u user -p pass -he 20 -w 20 -m 30
    new_game_subcommand = parser_game_subcommands.add_parser('new', help='New game')
    new_game_subcommand.add_argument('--height', '-he', type=int, dest='height',
                                     default='10',
                                     help='Height. Default: 10')
    new_game_subcommand.add_argument('--width', '-w', type=int, dest='width',
                                     default='10',
                                     help='Width. Default: 10')
    new_game_subcommand.add_argument('--mines', '-m', type=int, dest='mines',
                                     default='10',
                                     help='Number of mines. Default: 10')
    add_credentials_args(new_game_subcommand)

    # sub-command game play
    # e.g. game play -u user -p pass -x 5 -y 25
    play_game_subcommand = parser_game_subcommands.add_parser('play', help='Play game')
    play_game_subcommand.add_argument('-x', required=True, type=int, dest='x',
                                      help='X position')
    play_game_subcommand.add_argument('-y', required=True, type=int, dest='y',
                                      help='Y position')
    play_game_subcommand.add_argument('--id', '-i', required=True, type=int, dest='game_id',
                                      help='Game ID')
    play_game_subcommand.add_argument('--debug', default=False, dest='game_debug', action="store_true",
                                      help='Show mines in the board')
    play_game_subcommand.add_argument('--action', '-a', nargs='?', dest='game_action',
                                      choices=[game_action_reveal, game_action_red_flag, game_action_question_flag,
                                               game_action_clean],
                                      default=game_action_reveal,
                                      help='Action. Default: reveal')
    add_credentials_args(play_game_subcommand)

    # sub-command game play
    # e.g. game play -u user -p pass -x 5 -y 25
    draw_game_subcommand = parser_game_subcommands.add_parser('draw', help='Draw the game')
    draw_game_subcommand.add_argument('--id', '-i', required=True, type=int, dest='game_id',
                                      help='Game ID')
    draw_game_subcommand.add_argument('--debug', dest='game_debug', action="store_true",
                                      help='Show mines in the board')
    add_credentials_args(draw_game_subcommand)
    return parser


def add_credentials_args(parser):
    parser.add_argument('--user', '-u', required=True,
                        type=str, dest='user',
                        help='Username')
    parser.add_argument('--password', '-p', required=True,
                        type=str, dest='password',
                        help='Password')


def subcommand_user(api_client, args):
    r = ''
    if args.user_action == user_action_new:
        r = api_client.new_player(args.user, args.password)
    elif args.user_action == user_action_details:
        r = api_client.player_details()
    elif args.user_action == user_action_delete:
        r = api_client.player_delete()
    print(r)


def subcommand_game_new(api_client, args):
    r = api_client.new_game(args.height, args.width, args.mines)
    print(r)
    print(api_client.draw(r['id'], debug=False))


def subcommand_game_play(api_client, args):
    r = {}
    if args.game_action == game_action_reveal:
        r = api_client.reveal(args.game_id, args.x, args.y)
    elif args.game_action == game_action_red_flag:
        r = api_client.add_red_flag(args.game_id, args.x, args.y)
    elif args.game_action == game_action_question_flag:
        r = api_client.add_question_flag(args.game_id, args.x, args.y)
    elif args.game_action == game_action_clean:
        r = api_client.clean_flag(args.game_id, args.x, args.y)
    if r['state'] == 'Lost':
        print('Lost the game :(\n')
        print(api_client.draw(args.game_id, debug=True))
    elif r['state'] == 'Won':
        print('Won the game :D\n')
        print(api_client.draw(args.game_id, debug=True))
    else:
        print(api_client.draw(args.game_id, debug=args.game_debug))


def subcommand_game_draw(api_client, args):
    print(api_client.draw(args.game_id, args.game_debug))


class Credentials:

    def __init__(self, user, password):
        self.user = user
        self.password = password


class MinesweeperAPIClient:

    def __init__(self, user, password):
        self.user = user
        self.password = password
        self.session = requests.Session()
        self.session.auth = (user, password)

    def new_player(self, user, password):
        r = requests.post(f'{api_hostname}/player', json={'username': user, 'password': password})
        self.handle_error(r)
        return r.json()

    def player_details(self):
        r = self.session.get(f'{api_hostname}/player')
        self.handle_error(r)
        return r.json()

    def player_delete(self):
        r = self.session.delete(f'{api_hostname}/player')
        self.handle_error(r)
        return r.json()

    def new_game(self, height, width, mines):
        r = self.session.post(f'{api_hostname}/games', json={'height': height, 'width': width, 'mines': mines})
        self.handle_error(r)
        return r.json()

    def reveal(self, game_id, x, y):
        return self.action_on_a_cell(game_id, x, y, 'reveal')

    def add_red_flag(self, game_id, x, y):
        return self.action_on_a_cell(game_id, x, y, 'set-red-flag')

    def add_question_flag(self, game_id, x, y):
        return self.action_on_a_cell(game_id, x, y, 'set-question-flag')

    def clean_flag(self, x, y):
        return self.action_on_a_cell(x, y, 'set-red-flag')

    def action_on_a_cell(self, game_id, x, y, action):
        r = self.session.patch(f'{api_hostname}/games/{game_id}',
                               json={'action': action, 'position': {'x': x, 'y': y}})
        self.handle_error(r)
        return r.json()

    def draw(self, game_id, debug):
        params = {}
        if debug:
            params = {'debug': 'true'}
        r = self.session.get(f'{api_hostname}/games/{game_id}/ascii', params=params)
        self.handle_error(r)
        return r.text

    @staticmethod
    def handle_error(r):
        if r.status_code >= 300:
            raise ApiUnexpectedResponseError(r.status_code, r.text)


class ApiUnexpectedResponseError(Exception):

    def __init__(self, status_code, body, message="Unexpected API response"):
        self.status_code = status_code
        self.body = body
        self.message = f'{message}\nStatus code: {status_code}\nBody: {body}'
        super().__init__(self.message)


if __name__ == "__main__":
    run()
