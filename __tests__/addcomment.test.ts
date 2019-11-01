import cmt from '../src/AddComment';

beforeEach(() => {
    process.env.GITHUB_REPOSITORY = 'a/b/c'
});

jest.mock('../src/AddComment');
test('getOwnerAndRepo', () => {

    const  value : string[]  = new cmt().getOwnerAndRepo();

    expect(value[0]).toBe('a');
} );


test('', () => {

});

